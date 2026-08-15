package com.example.transport

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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class WifiBridgeTransport(
    private val scope: CoroutineScope
) : IBridgeTransport {

    companion object {
        private const val MAGIC_0: Byte = 0x55
        private const val MAGIC_1: Byte = 0xAA.toByte()
        private const val CMD_GET_STATUS: Byte = 0x03
        private const val CMD_START_TRIGGER: Byte = 0x10
        private const val CMD_STOP_TRIGGER: Byte = 0x11
        private const val CMD_START_UART_BRIDGE: Byte = 0x20
        private const val CMD_STOP_UART_BRIDGE: Byte = 0x21
        private const val CMD_RAW_UART_DATA: Byte = 0x30
    }

    private var webSocket: WebSocket? = null
    private var tcpSocket: Socket? = null
    private var tcpReadJob: Job? = null

    private val _connectionState = MutableStateFlow<BridgeConnectionState>(BridgeConnectionState.Disconnected)
    override val connectionState: StateFlow<BridgeConnectionState> = _connectionState.asStateFlow()

    private val _incomingFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingFrames: SharedFlow<ByteArray> = _incomingFrames.asSharedFlow()

    private val _bridgeStatus = MutableStateFlow(
        BridgeStatus(
            isConnected = false,
            transportType = TransportType.WIFI_SOFTAP,
            deviceName = "ESP32-S3 SoftAP"
        )
    )
    override val bridgeStatus: StateFlow<BridgeStatus> = _bridgeStatus.asStateFlow()

    private var currentSeq: Byte = 0

    override suspend fun connect(targetParam: String): Boolean = withContext(Dispatchers.IO) {
        val targetIp = if (targetParam.isNotBlank()) targetParam.trim() else "192.168.4.1"
        _connectionState.value = BridgeConnectionState.Connecting

        try {
            // First attempt WebSocket connection on port 8080
            val wsUrl = "ws://$targetIp:8080/"
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder().url(wsUrl).build()
            
            var wsConnected = false
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    wsConnected = true
                    _connectionState.value = BridgeConnectionState.Connected("Wi-Fi WebSocket Connected ($targetIp:8080)")
                    _bridgeStatus.value = _bridgeStatus.value.copy(
                        isConnected = true,
                        transportType = TransportType.WIFI_SOFTAP,
                        ipAddress = targetIp
                    )
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    _incomingFrames.tryEmit(bytes.toByteArray())
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _connectionState.value = BridgeConnectionState.Error("Wi-Fi Error: ${t.message}")
                    _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = false)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _connectionState.value = BridgeConnectionState.Disconnected
                    _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = false)
                }
            }

            webSocket = client.newWebSocket(request, listener)
            
            // Wait up to 3 seconds for WS handshake
            var waited = 0
            while (!wsConnected && waited < 30) {
                kotlinx.coroutines.delay(100)
                waited++
                if (_connectionState.value is BridgeConnectionState.Error) break
            }

            if (wsConnected) {
                requestStatus()
                return@withContext true
            }

            // Fallback to raw TCP socket on port 8888
            val socket = Socket()
            socket.connect(InetSocketAddress(targetIp, 8888), 4000)
            socket.tcpNoDelay = true
            tcpSocket = socket

            _connectionState.value = BridgeConnectionState.Connected("Wi-Fi TCP Connected ($targetIp:8888)")
            _bridgeStatus.value = _bridgeStatus.value.copy(
                isConnected = true,
                transportType = TransportType.WIFI_SOFTAP,
                ipAddress = targetIp
            )

            startTcpReadLoop(socket)
            requestStatus()
            return@withContext true
        } catch (e: Exception) {
            _connectionState.value = BridgeConnectionState.Error("Failed to connect to ESP32 Wi-Fi ($targetIp): ${e.message}")
            return@withContext false
        }
    }

    private fun startTcpReadLoop(socket: Socket) {
        tcpReadJob?.cancel()
        tcpReadJob = scope.launch(Dispatchers.IO) {
            val stream = socket.getInputStream()
            val buffer = ByteArray(4096)
            while (isActive && socket.isConnected && !socket.isClosed) {
                val read = stream.read(buffer)
                if (read > 0) {
                    _incomingFrames.emit(buffer.copyOf(read))
                } else if (read == -1) {
                    break
                }
            }
            _connectionState.value = BridgeConnectionState.Disconnected
            _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = false)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        tcpReadJob?.cancel()
        tcpReadJob = null
        try {
            tcpSocket?.close()
        } catch (_: Exception) {}
        tcpSocket = null

        _connectionState.value = BridgeConnectionState.Disconnected
        _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = false)
    }

    override suspend fun sendFrame(cmdId: Byte, seq: Byte, payload: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val frame = buildFrame(cmdId, seq, payload)
        
        webSocket?.let { ws ->
            return@withContext ws.send(frame.toByteString())
        }

        tcpSocket?.let { socket ->
            try {
                socket.getOutputStream().write(frame)
                socket.getOutputStream().flush()
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            }
        }

        return@withContext false
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

        var crc = 0xFFFF
        val raw = buffer.array()
        for (i in 0 until totalWithoutCrc) {
            crc = crc xor ((raw[i].toInt() and 0xFF) shl 8)
            for (j in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
            }
        }
        buffer.putShort((crc and 0xFFFF).toShort())
        return buffer.array()
    }
}
