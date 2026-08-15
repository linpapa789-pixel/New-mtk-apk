package com.example.transport

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
import com.example.model.BridgeStatus
import com.example.model.OperationalRole
import com.example.model.TransportType
import com.example.model.TriggerConfig
import com.example.model.UartConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UsbBridgeTransport(
    private val context: Context,
    private val scope: CoroutineScope
) : IBridgeTransport {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.mtkbridge.USB_PERMISSION"
        private const val MAGIC_0: Byte = 0x55
        private const val MAGIC_1: Byte = 0xAA.toByte()

        private const val CMD_PING: Byte = 0x01
        private const val CMD_GET_STATUS: Byte = 0x03
        private const val CMD_START_TRIGGER: Byte = 0x10
        private const val CMD_STOP_TRIGGER: Byte = 0x11
        private const val CMD_START_UART_BRIDGE: Byte = 0x20
        private const val CMD_STOP_UART_BRIDGE: Byte = 0x21
        private const val CMD_RAW_UART_DATA: Byte = 0x30
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null
    private var readJob: Job? = null

    private val _connectionState = MutableStateFlow<BridgeConnectionState>(BridgeConnectionState.Disconnected)
    override val connectionState: StateFlow<BridgeConnectionState> = _connectionState.asStateFlow()

    private val _incomingFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingFrames: SharedFlow<ByteArray> = _incomingFrames.asSharedFlow()

    private val _bridgeStatus = MutableStateFlow(
        BridgeStatus(
            isConnected = false,
            transportType = TransportType.USB_CDC,
            deviceName = "ESP32-S3 USB CDC"
        )
    )
    override val bridgeStatus: StateFlow<BridgeStatus> = _bridgeStatus.asStateFlow()

    private var currentSeq: Byte = 0

    override suspend fun connect(targetParam: String): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = BridgeConnectionState.Connecting

        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            _connectionState.value = BridgeConnectionState.Error("No USB devices detected. Connect ESP32-S3 via OTG.")
            return@withContext false
        }

        // Find ESP32-S3 (Espressif VID 0x303A or standard CDC ACM)
        var targetDevice: UsbDevice? = null
        for ((_, device) in deviceList) {
            if (device.vendorId == 0x303A || device.deviceClass == UsbConstants.USB_CLASS_COMM || device.deviceClass == UsbConstants.USB_CLASS_MISC) {
                targetDevice = device
                break
            }
        }

        if (targetDevice == null) {
            targetDevice = deviceList.values.firstOrNull()
        }

        if (targetDevice == null) {
            _connectionState.value = BridgeConnectionState.Error("ESP32-S3 USB device not found.")
            return@withContext false
        }

        if (!usbManager.hasPermission(targetDevice)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(targetDevice, permissionIntent)
            _connectionState.value = BridgeConnectionState.Error("Requested USB permission for ESP32-S3. Please accept popup and retry.")
            return@withContext false
        }

        try {
            val connection = usbManager.openDevice(targetDevice)
                ?: run {
                    _connectionState.value = BridgeConnectionState.Error("Failed to open USB device.")
                    return@withContext false
                }

            // Find data interface with IN and OUT endpoints
            var dataInterface: UsbInterface? = null
            var epIn: UsbEndpoint? = null
            var epOut: UsbEndpoint? = null

            for (i in 0 until targetDevice.interfaceCount) {
                val iface = targetDevice.getInterface(i)
                var tempIn: UsbEndpoint? = null
                var tempOut: UsbEndpoint? = null

                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) {
                            tempIn = ep
                        } else {
                            tempOut = ep
                        }
                    }
                }

                if (tempIn != null && tempOut != null) {
                    dataInterface = iface
                    epIn = tempIn
                    epOut = tempOut
                    break
                }
            }

            if (dataInterface == null || epIn == null || epOut == null) {
                connection.close()
                _connectionState.value = BridgeConnectionState.Error("No valid CDC Bulk endpoints found on device.")
                return@withContext false
            }

            connection.claimInterface(dataInterface, true)
            
            // Set CDC line coding (115200 8N1) via control transfer
            val lineCoding = byteArrayOf(
                0x00, 0xC2.toByte(), 0x01, 0x00, // 115200 baud
                0x00, // 1 stop bit
                0x00, // No parity
                0x08  // 8 data bits
            )
            connection.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.size, 1000)
            // Set DTR + RTS
            connection.controlTransfer(0x21, 0x22, 0x03, 0, null, 0, 1000)

            usbConnection = connection
            usbInterface = dataInterface
            inEndpoint = epIn
            outEndpoint = epOut

            _connectionState.value = BridgeConnectionState.Connected("USB-CDC Attached (${targetDevice.productName ?: "ESP32-S3"})")
            _bridgeStatus.value = _bridgeStatus.value.copy(
                isConnected = true,
                transportType = TransportType.USB_CDC,
                deviceName = targetDevice.productName ?: "ESP32-S3"
            )

            startReadLoop()
            requestStatus()
            return@withContext true
        } catch (e: Exception) {
            _connectionState.value = BridgeConnectionState.Error("USB Open Exception: ${e.message}")
            return@withContext false
        }
    }

    private fun startReadLoop() {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            while (isActive && usbConnection != null && inEndpoint != null) {
                val bytesRead = usbConnection?.bulkTransfer(inEndpoint, buffer, buffer.size, 200) ?: -1
                if (bytesRead > 0) {
                    val frameCopy = buffer.copyOf(bytesRead)
                    _incomingFrames.emit(frameCopy)
                }
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        readJob?.cancel()
        readJob = null
        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
            usbConnection?.close()
        } catch (_: Exception) {}
        usbConnection = null
        usbInterface = null
        inEndpoint = null
        outEndpoint = null
        _connectionState.value = BridgeConnectionState.Disconnected
        _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = false)
    }

    override suspend fun sendFrame(cmdId: Byte, seq: Byte, payload: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val conn = usbConnection ?: return@withContext false
        val ep = outEndpoint ?: return@withContext false

        val frame = buildFrame(cmdId, seq, payload)
        val written = conn.bulkTransfer(ep, frame, frame.size, 1000)
        return@withContext (written == frame.size)
    }

    override suspend fun startTrigger(config: TriggerConfig): Boolean {
        val buf = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
        buf.putShort(config.durationMs.toShort())
        buf.put(config.pulseCount.toByte())
        buf.put((if (config.activeLow) 0 else 1).toByte())
        buf.put((if (config.useRelay) 1 else 0).toByte())

        val ok = sendFrame(CMD_START_TRIGGER, currentSeq++, buf.array())
        if (ok) {
            _bridgeStatus.value = _bridgeStatus.value.copy(triggerActive = true)
        }
        return ok
    }

    override suspend fun stopTrigger(): Boolean {
        val ok = sendFrame(CMD_STOP_TRIGGER, currentSeq++, byteArrayOf())
        if (ok) {
            _bridgeStatus.value = _bridgeStatus.value.copy(triggerActive = false)
        }
        return ok
    }

    override suspend fun startUartBridge(config: UartConfig): Boolean {
        val buf = ByteBuffer.allocate(7).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(config.baudRate.toInt())
        buf.put(config.dataBits.toByte())
        buf.put(config.parity.toByte())
        buf.put(config.stopBits.toByte())

        val ok = sendFrame(CMD_START_UART_BRIDGE, currentSeq++, buf.array())
        if (ok) {
            _bridgeStatus.value = _bridgeStatus.value.copy(
                uartBridgeActive = true,
                roleMode = OperationalRole.UART_BRIDGE,
                activeBaud = config.baudRate
            )
        }
        return ok
    }

    override suspend fun stopUartBridge(): Boolean {
        val ok = sendFrame(CMD_STOP_UART_BRIDGE, currentSeq++, byteArrayOf())
        if (ok) {
            _bridgeStatus.value = _bridgeStatus.value.copy(
                uartBridgeActive = false,
                roleMode = OperationalRole.TEST_POINT_TRIGGER
            )
        }
        return ok
    }

    override suspend fun sendRawUart(data: ByteArray): Boolean {
        return sendFrame(CMD_RAW_UART_DATA, currentSeq++, data)
    }

    override suspend fun requestStatus(): Boolean {
        return sendFrame(CMD_GET_STATUS, currentSeq++, byteArrayOf())
    }

    private fun buildFrame(cmdId: Byte, seq: Byte, payload: ByteArray): ByteArray {
        val payloadLen = payload.size
        val totalWithoutCrc = 6 + payloadLen
        val buffer = ByteBuffer.allocate(totalWithoutCrc + 2).order(ByteOrder.BIG_ENDIAN)

        buffer.put(MAGIC_0)
        buffer.put(MAGIC_1)
        buffer.put(cmdId)
        buffer.put(seq)
        buffer.putShort(payloadLen.toShort())
        if (payloadLen > 0) {
            buffer.put(payload)
        }

        // Calculate CRC16 CCITT
        val crc = crc16Ccitt(buffer.array(), 0, totalWithoutCrc)
        buffer.putShort(crc.toShort())
        return buffer.array()
    }

    private fun crc16Ccitt(data: ByteArray, offset: Int, length: Int): Int {
        var crc = 0xFFFF
        for (i in offset until (offset + length)) {
            crc = crc xor ((data[i].toInt() and 0xFF) shl 8)
            for (j in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
            }
        }
        return crc and 0xFFFF
    }
}
