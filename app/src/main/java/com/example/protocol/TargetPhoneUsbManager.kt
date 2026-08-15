package com.example.protocol

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class TargetPhoneState {
    object Disconnected : TargetPhoneState()
    object RequestingPermission : TargetPhoneState()
    data class Connected(val deviceName: String, val isBromMode: Boolean, val vidPid: String) : TargetPhoneState()
    data class Error(val message: String) : TargetPhoneState()
}

class TargetPhoneUsbManager(
    private val context: Context
) {
    companion object {
        const val ACTION_USB_PHONE_PERMISSION = "com.example.mtkbridge.USB_PHONE_PERMISSION"
        const val MTK_VID = 0x0E8D // MediaTek Inc
        const val MTK_PID_BROM = 0x0003 // MTK USB Port (BROM Mode)
        const val MTK_PID_PRELOADER = 0x2000 // MTK DA / Preloader USB VCOM Port
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null

    private val _phoneState = MutableStateFlow<TargetPhoneState>(TargetPhoneState.Disconnected)
    val phoneState: StateFlow<TargetPhoneState> = _phoneState.asStateFlow()

    suspend fun scanAndConnect(): Boolean = withContext(Dispatchers.IO) {
        val deviceList = usbManager.deviceList
        var mtkDevice: UsbDevice? = null

        for ((_, device) in deviceList) {
            if (device.vendorId == MTK_VID || device.productId == MTK_PID_BROM || device.productId == MTK_PID_PRELOADER) {
                mtkDevice = device
                break
            }
        }

        if (mtkDevice == null) {
            _phoneState.value = TargetPhoneState.Disconnected
            return@withContext false
        }

        if (!usbManager.hasPermission(mtkDevice)) {
            _phoneState.value = TargetPhoneState.RequestingPermission
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PHONE_PERMISSION), flags
            )
            usbManager.requestPermission(mtkDevice, permissionIntent)
            return@withContext false
        }

        try {
            val connection = usbManager.openDevice(mtkDevice) ?: run {
                _phoneState.value = TargetPhoneState.Error("Failed to open target MediaTek phone USB port.")
                return@withContext false
            }

            var bulkIn: UsbEndpoint? = null
            var bulkOut: UsbEndpoint? = null
            var claimedIface: UsbInterface? = null

            for (i in 0 until mtkDevice.interfaceCount) {
                val iface = mtkDevice.getInterface(i)
                var tempIn: UsbEndpoint? = null
                var tempOut: UsbEndpoint? = null

                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) tempIn = ep
                        else tempOut = ep
                    }
                }

                if (tempIn != null && tempOut != null) {
                    claimedIface = iface
                    bulkIn = tempIn
                    bulkOut = tempOut
                    break
                }
            }

            if (claimedIface == null || bulkIn == null || bulkOut == null) {
                connection.close()
                _phoneState.value = TargetPhoneState.Error("MediaTek USB endpoints not found.")
                return@withContext false
            }

            connection.claimInterface(claimedIface, true)
            usbConnection = connection
            usbInterface = claimedIface
            inEndpoint = bulkIn
            outEndpoint = bulkOut

            val isBrom = (mtkDevice.productId == MTK_PID_BROM)
            val vidPidStr = String.format("0x%04X:0x%04X", mtkDevice.vendorId, mtkDevice.productId)
            val modeName = if (isBrom) "BROM Mode (0x0003)" else "Preloader Mode (0x2000)"

            _phoneState.value = TargetPhoneState.Connected(
                deviceName = mtkDevice.productName ?: "MediaTek Handset",
                isBromMode = isBrom,
                vidPid = "$vidPidStr [$modeName]"
            )
            return@withContext true
        } catch (e: Exception) {
            _phoneState.value = TargetPhoneState.Error("Target USB Error: ${e.message}")
            return@withContext false
        }
    }

    fun writeRaw(bytes: ByteArray, timeoutMs: Int = 1000): Int {
        val conn = usbConnection ?: return -1
        val ep = outEndpoint ?: return -1
        return conn.bulkTransfer(ep, bytes, bytes.size, timeoutMs)
    }

    fun readRaw(buffer: ByteArray, timeoutMs: Int = 1000): Int {
        val conn = usbConnection ?: return -1
        val ep = inEndpoint ?: return -1
        return conn.bulkTransfer(ep, buffer, buffer.size, timeoutMs)
    }

    fun disconnect() {
        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
            usbConnection?.close()
        } catch (_: Exception) {}
        usbConnection = null
        usbInterface = null
        inEndpoint = null
        outEndpoint = null
        _phoneState.value = TargetPhoneState.Disconnected
    }

    fun isConnected(): Boolean = (usbConnection != null)
}
