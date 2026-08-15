package com.example.transport

import com.example.model.BridgeStatus
import com.example.model.TriggerConfig
import com.example.model.UartConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class BridgeConnectionState {
    object Disconnected : BridgeConnectionState()
    object Connecting : BridgeConnectionState()
    data class Connected(val info: String) : BridgeConnectionState()
    data class Error(val message: String) : BridgeConnectionState()
}

interface IBridgeTransport {
    val connectionState: StateFlow<BridgeConnectionState>
    val incomingFrames: Flow<ByteArray>
    val bridgeStatus: StateFlow<BridgeStatus>

    suspend fun connect(targetParam: String = ""): Boolean
    suspend fun disconnect()
    suspend fun sendFrame(cmdId: Byte, seq: Byte, payload: ByteArray): Boolean
    suspend fun startTrigger(config: TriggerConfig): Boolean
    suspend fun stopTrigger(): Boolean
    suspend fun startUartBridge(config: UartConfig): Boolean
    suspend fun stopUartBridge(): Boolean
    suspend fun sendRawUart(data: ByteArray): Boolean
    suspend fun requestStatus(): Boolean
}
