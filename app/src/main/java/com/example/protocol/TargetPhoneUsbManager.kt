package com.example.protocol

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class TargetPhoneState {
    object Disconnected : TargetPhoneState()
    object RequestingPermission : TargetPhoneState()
    data class Connected(
        val deviceName: String,
        val isBromMode: Boolean,
        val vidPid: String,
        val fileDescriptor: Int = -1
    ) : TargetPhoneState()
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
        const val MTK_PID_PRELOADER_2 = 0x2001
        const val MTK_PID_CDC = 0x2004
        const val MTK_PID_DEBUG = 0x2005
        const val MTK_PID_BOOTROM_GENERIC = 0x0001
        const val MTK_PID_DA_HIGH_SPEED = 0x0002
        const val MTK_PID_PRELOADER_ALT = 0x0005

        // Known USB Vendor IDs used by various MTK devices & flashing cables
        val SUPPORTED_VIDS = setOf(
            0x0E8D, // MediaTek Inc
            0x1004, // LG Electronics MTK
            0x0BB4, // HTC MTK
            0x2A45, // Meizu MTK
            0x1782, // Spreadtrum/MTK fallback
            0x1A86, // CH340 / USB Serial converter (if using OTG bridge)
            0x10C4, // CP210x Serial (if using testpoint jig)
            0x0403  // FTDI Serial
        )
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var onDeviceAutoConnectedListener: ((TargetPhoneState.Connected) -> Unit)? = null

    private val _phoneState = MutableStateFlow<TargetPhoneState>(TargetPhoneState.Disconnected)
    val phoneState: StateFlow<TargetPhoneState> = _phoneState.asStateFlow()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                ACTION_USB_PHONE_PERMISSION -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted && device != null) {
                        scope.launch {
                            connectDevice(device)
                        }
                    } else {
                        _phoneState.value = TargetPhoneState.Error("USB Permission denied by user.")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (device != null && isMediaTekDevice(device)) {
                        scope.launch {
                            if (usbManager.hasPermission(device)) {
                                connectDevice(device)
                            } else {
                                requestDevicePermission(device)
                            }
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (device != null && isMediaTekDevice(device)) {
                        disconnect()
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PHONE_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }

    fun isMediaTekDevice(device: UsbDevice): Boolean {
        if (SUPPORTED_VIDS.contains(device.vendorId)) return true
        if (device.vendorId == MTK_VID) return true
        // Also check device class / subclass or interface class for CDC/Communication/Vendor device
        if (device.deviceClass == UsbConstants.USB_CLASS_COMM || device.deviceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) return true
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA ||
                iface.interfaceClass == UsbConstants.USB_CLASS_COMM ||
                iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                return true
            }
        }
        return false
    }

    fun requestDevicePermission(device: UsbDevice) {
        _phoneState.value = TargetPhoneState.RequestingPermission
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PHONE_PERMISSION), flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    suspend fun scanAndConnect(): Boolean = withContext(Dispatchers.IO) {
        val deviceList = usbManager.deviceList
        var mtkDevice: UsbDevice? = null

        for ((_, device) in deviceList) {
            if (isMediaTekDevice(device)) {
                mtkDevice = device
                break
            }
        }

        // If no explicit MTK device found by VID, but there is any attached USB device (e.g. OTG plugged), try to use it
        if (mtkDevice == null && deviceList.isNotEmpty()) {
            mtkDevice = deviceList.values.firstOrNull()
        }

        if (mtkDevice == null) {
            _phoneState.value = TargetPhoneState.Disconnected
            return@withContext false
        }

        if (!usbManager.hasPermission(mtkDevice)) {
            requestDevicePermission(mtkDevice)
            return@withContext false
        }

        return@withContext connectDevice(mtkDevice)
    }

    suspend fun connectDevice(mtkDevice: UsbDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = usbManager.openDevice(mtkDevice) ?: run {
                _phoneState.value = TargetPhoneState.Error("Failed to open target USB port (OTG connection refused).")
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

            // Fallback: if no single interface had both in/out bulk endpoints, check across interfaces or use endpoints
            if (claimedIface == null || bulkIn == null || bulkOut == null) {
                for (i in 0 until mtkDevice.interfaceCount) {
                    val iface = mtkDevice.getInterface(i)
                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.direction == UsbConstants.USB_DIR_IN && bulkIn == null) {
                            bulkIn = ep
                            if (claimedIface == null) claimedIface = iface
                        } else if (ep.direction == UsbConstants.USB_DIR_OUT && bulkOut == null) {
                            bulkOut = ep
                            if (claimedIface == null) claimedIface = iface
                        }
                    }
                }
            }

            if (claimedIface == null || bulkIn == null || bulkOut == null) {
                connection.close()
                _phoneState.value = TargetPhoneState.Error("USB Bulk Endpoints not found for ${mtkDevice.productName ?: "Device"}.")
                return@withContext false
            }

            connection.claimInterface(claimedIface, true)
            usbConnection = connection
            usbInterface = claimedIface
            inEndpoint = bulkIn
            outEndpoint = bulkOut

            val isBrom = (mtkDevice.productId == MTK_PID_BROM) || (mtkDevice.productId == 0x0001) || (mtkDevice.productId == 0x0003)
            val vidPidStr = String.format("0x%04X:0x%04X", mtkDevice.vendorId, mtkDevice.productId)
            val modeName = if (isBrom) "BROM Mode (0x0003)" else "Preloader / USB VCOM"
            val rawFd = connection.fileDescriptor

            val state = TargetPhoneState.Connected(
                deviceName = mtkDevice.productName ?: "MediaTek Device",
                isBromMode = isBrom,
                vidPid = "$vidPidStr [$modeName]",
                fileDescriptor = rawFd
            )
            _phoneState.value = state
            onDeviceAutoConnectedListener?.invoke(state)
            return@withContext true
        } catch (e: Exception) {
            _phoneState.value = TargetPhoneState.Error("Target USB Error: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Fast-blasts the MTK handshake sync sequence (0xA0 0x0A 0x50 0x05)
     * repeatedly to hook BROM before the device exits bootrom mode.
     */
    fun blastBromHandshakeSync(maxAttempts: Int = 15): Boolean {
        val conn = usbConnection ?: return false
        val outEp = outEndpoint ?: return false
        val inEp = inEndpoint ?: return false

        val syncSeq = byteArrayOf(0xA0.toByte(), 0x0A.toByte(), 0x50.toByte(), 0x05.toByte())
        val rxBuf = ByteArray(4)

        for (i in 0 until maxAttempts) {
            conn.bulkTransfer(outEp, syncSeq, syncSeq.size, 150)
            val read = conn.bulkTransfer(inEp, rxBuf, rxBuf.size, 150)
            if (read >= 4) {
                // Expected response is 0x5F 0xF5 0xAF 0xFA (or inverted handshake echo)
                return true
            }
        }
        return false
    }

    fun getFileDescriptor(): Int {
        return usbConnection?.fileDescriptor ?: -1
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

    fun controlTransfer(
        requestType: Int,
        request: Int,
        value: Int,
        index: Int,
        buffer: ByteArray?,
        length: Int,
        timeoutMs: Int = 1000
    ): Int {
        val conn = usbConnection ?: return -1
        return conn.controlTransfer(requestType, request, value, index, buffer, length, timeoutMs)
    }

    fun sendWatchdogResetControl(): Boolean {
        val conn = usbConnection ?: return false
        val res = conn.controlTransfer(
            0x40, // USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_DIR_OUT
            0x01, // Request
            0x0000,
            0x0000,
            null,
            0,
            1000
        )
        return res >= 0
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

    fun unregister() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }

    fun isConnected(): Boolean = (usbConnection != null)
}

