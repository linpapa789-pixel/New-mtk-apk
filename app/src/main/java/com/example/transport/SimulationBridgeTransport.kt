package com.example.transport

import com.example.model.BridgeStatus
import com.example.model.OperationalRole
import com.example.model.TransportType
import com.example.model.TriggerConfig
import com.example.model.UartConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SimulationBridgeTransport : IBridgeTransport {

    private val _connectionState = MutableStateFlow<BridgeConnectionState>(
        BridgeConnectionState.Connected("Simulation Bridge (Dry-Run Mode)")
    )
    override val connectionState: StateFlow<BridgeConnectionState> = _connectionState.asStateFlow()

    private val _incomingFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingFrames: SharedFlow<ByteArray> = _incomingFrames.asSharedFlow()

    private val _bridgeStatus = MutableStateFlow(
        BridgeStatus(
            isConnected = true,
            transportType = TransportType.SIMULATION,
            deviceName = "ESP32-S3 (Simulated N16R8)",
            firmwareVersion = "1.0.0-SIM",
            uptimeSec = 1420,
            roleMode = OperationalRole.TEST_POINT_TRIGGER,
            triggerActive = false,
            uartBridgeActive = false,
            activeBaud = 115200,
            activeClients = 1,
            ipAddress = "192.168.4.1"
        )
    )
    override val bridgeStatus: StateFlow<BridgeStatus> = _bridgeStatus.asStateFlow()

    override suspend fun connect(targetParam: String): Boolean {
        _connectionState.value = BridgeConnectionState.Connecting
        delay(300)
        _connectionState.value = BridgeConnectionState.Connected("Simulation Bridge (Dry-Run Mode)")
        _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = true)
        return true
    }

    override suspend fun disconnect() {
        _connectionState.value = BridgeConnectionState.Disconnected
        _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = false)
    }

    override suspend fun sendFrame(cmdId: Byte, seq: Byte, payload: ByteArray): Boolean {
        return true
    }

    override suspend fun startTrigger(config: TriggerConfig): Boolean {
        _bridgeStatus.value = _bridgeStatus.value.copy(triggerActive = true)
        delay(config.durationMs.toLong())
        _bridgeStatus.value = _bridgeStatus.value.copy(triggerActive = false)
        return true
    }

    override suspend fun stopTrigger(): Boolean {
        _bridgeStatus.value = _bridgeStatus.value.copy(triggerActive = false)
        return true
    }

    override suspend fun startUartBridge(config: UartConfig): Boolean {
        _bridgeStatus.value = _bridgeStatus.value.copy(
            uartBridgeActive = true,
            roleMode = OperationalRole.UART_BRIDGE,
            activeBaud = config.baudRate
        )
        return true
    }

    override suspend fun stopUartBridge(): Boolean {
        _bridgeStatus.value = _bridgeStatus.value.copy(
            uartBridgeActive = false,
            roleMode = OperationalRole.TEST_POINT_TRIGGER
        )
        return true
    }

    override suspend fun sendRawUart(data: ByteArray): Boolean {
        return true
    }

    override suspend fun requestStatus(): Boolean {
        return true
    }
}
