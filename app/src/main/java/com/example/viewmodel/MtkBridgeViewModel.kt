package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiDiagnosticAdvisor
import com.example.model.BridgeStatus
import com.example.model.LogLevel
import com.example.model.MtkChipInfo
import com.example.model.OperationProgress
import com.example.model.PartitionEntry
import com.example.model.ServiceFunction
import com.example.model.TerminalLog
import com.example.model.TransportType
import com.example.model.TriggerConfig
import com.example.parser.ScatterParser
import com.example.protocol.MtkBromProtocolEngine
import com.example.protocol.TargetPhoneState
import com.example.protocol.TargetPhoneUsbManager
import com.example.storage.BackupStorageManager
import com.example.transport.BridgeConnectionState
import com.example.transport.IBridgeTransport
import com.example.transport.SimulationBridgeTransport
import com.example.transport.UsbBridgeTransport
import com.example.transport.WifiBridgeTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MtkBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val storageManager = BackupStorageManager(application)
    val targetPhoneUsb = TargetPhoneUsbManager(application)
    private val aiAdvisor = GeminiDiagnosticAdvisor()

    private var usbTransport: UsbBridgeTransport = UsbBridgeTransport(application, viewModelScope)
    private var wifiTransport: WifiBridgeTransport = WifiBridgeTransport(viewModelScope)
    private var simTransport: SimulationBridgeTransport = SimulationBridgeTransport()

    private var activeTransport: IBridgeTransport = simTransport

    // UI States
    private val _selectedTransportType = MutableStateFlow(TransportType.SIMULATION)
    val selectedTransportType: StateFlow<TransportType> = _selectedTransportType.asStateFlow()

    private val _bridgeState = MutableStateFlow<BridgeConnectionState>(BridgeConnectionState.Connected("Simulation Mode Ready"))
    val bridgeState: StateFlow<BridgeConnectionState> = _bridgeState.asStateFlow()

    private val _bridgeStatus = MutableStateFlow(
        BridgeStatus(
            isConnected = true,
            transportType = TransportType.SIMULATION,
            deviceName = "ESP32-S3 (Simulated N16R8)"
        )
    )
    val bridgeStatus: StateFlow<BridgeStatus> = _bridgeStatus.asStateFlow()

    val targetPhoneState: StateFlow<TargetPhoneState> = targetPhoneUsb.phoneState

    private val _chipInfo = MutableStateFlow(MtkChipInfo())
    val chipInfo: StateFlow<MtkChipInfo> = _chipInfo.asStateFlow()

    private val _scatterPlatform = MutableStateFlow("MT6765")
    val scatterPlatform: StateFlow<String> = _scatterPlatform.asStateFlow()

    private val _partitions = MutableStateFlow<List<PartitionEntry>>(emptyList())
    val partitions: StateFlow<List<PartitionEntry>> = _partitions.asStateFlow()

    private val _selectedPartitionIndex = MutableStateFlow(2) // Defaults to nvram
    val selectedPartitionIndex: StateFlow<Int> = _selectedPartitionIndex.asStateFlow()

    private val _selectedServiceFunction = MutableStateFlow(ServiceFunction.READ_INFO)
    val selectedServiceFunction: StateFlow<ServiceFunction> = _selectedServiceFunction.asStateFlow()

    private val _isDryRun = MutableStateFlow(true)
    val isDryRun: StateFlow<Boolean> = _isDryRun.asStateFlow()

    private val _autoNvBackup = MutableStateFlow(true)
    val autoNvBackup: StateFlow<Boolean> = _autoNvBackup.asStateFlow()

    private val _autoReboot = MutableStateFlow(true)
    val autoReboot: StateFlow<Boolean> = _autoReboot.asStateFlow()

    private val _backupLocation = MutableStateFlow(storageManager.getBackupDirectory().absolutePath)
    val backupLocation: StateFlow<String> = _backupLocation.asStateFlow()

    private val _operationProgress = MutableStateFlow(OperationProgress())
    val operationProgress: StateFlow<OperationProgress> = _operationProgress.asStateFlow()

    private val _logs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val logs: StateFlow<List<TerminalLog>> = _logs.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<String?>(null)
    val aiAnalysis: StateFlow<String?> = _aiAnalysis.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Config StateFlows for UI components
    val triggerDurationMs = MutableStateFlow(500)
    val wifiIpAddress = MutableStateFlow("192.168.4.1")

    // File selection paths
    val daAgentPath = MutableStateFlow("Built-in Universal DA (MTK All-in-One)")
    val customDaPath = daAgentPath // alias
    val authFilePath = MutableStateFlow("")
    val preloaderPath = MutableStateFlow("")
    val scatterPath = MutableStateFlow("")

    private lateinit var protocolEngine: MtkBromProtocolEngine

    init {
        protocolEngine = MtkBromProtocolEngine(
            targetPhoneUsb = targetPhoneUsb,
            bridgeTransport = activeTransport,
            storageManager = storageManager,
            logCallback = { log -> addLog(log) },
            progressCallback = { prog -> _operationProgress.value = prog }
        )

        // Load default MT6765 preset partitions
        val defaultScatter = ScatterParser.getDefaultPreset("MT6765")
        _scatterPlatform.value = defaultScatter.first
        _partitions.value = defaultScatter.second

        addLog(TerminalLog(now(), "MTK BROM Flash Bridge Initialized.", LogLevel.SUCCESS))
        addLog(TerminalLog(now(), "Built-in Universal Download Agent (DA) loaded.", LogLevel.INFO))
        addLog(TerminalLog(now(), "Backup Directory: ${_backupLocation.value}", LogLevel.INFO))

        listenToActiveTransport()
    }

    private fun now(): String = timeFormat.format(Date())

    fun addLog(log: TerminalLog) {
        val current = _logs.value.toMutableList()
        current.add(log)
        if (current.size > 500) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addLog(TerminalLog(now(), "Terminal log cleared.", LogLevel.INFO))
    }

    fun toggleAutoReboot(enabled: Boolean) {
        _autoReboot.value = enabled
        addLog(TerminalLog(now(), "Post-Operation Auto Reboot: ${if (enabled) "ENABLED" else "DISABLED"}", LogLevel.INFO))
    }

    fun toggleAutoNvBackup(enabled: Boolean) {
        _autoNvBackup.value = enabled
        addLog(TerminalLog(now(), "Auto NV Data Backup Policy: ${if (enabled) "ENABLED (Backup will be created)" else "DISABLED (Backup will be skipped)"}", LogLevel.INFO))
    }

    fun setCustomBackupLocation(path: String) {
        storageManager.setCustomBackupPath(path)
        _backupLocation.value = storageManager.getBackupDirectory().absolutePath
        addLog(TerminalLog(now(), "Backup output path set to: ${_backupLocation.value}", LogLevel.SUCCESS))
    }

    fun setTransportType(type: TransportType) {
        if (_selectedTransportType.value == type) return
        _selectedTransportType.value = type
        viewModelScope.launch {
            activeTransport.disconnect()
            activeTransport = when (type) {
                TransportType.USB_CDC -> usbTransport
                TransportType.WIFI_SOFTAP -> wifiTransport
                TransportType.SIMULATION -> simTransport
            }
            listenToActiveTransport()
            addLog(TerminalLog(now(), "Switched transport interface to ${type.displayName}", LogLevel.INFO))
        }
    }

    fun connectBridge() {
        viewModelScope.launch {
            val param = if (_selectedTransportType.value == TransportType.WIFI_SOFTAP) wifiIpAddress.value else ""
            activeTransport.connect(param)
        }
    }

    fun disconnectBridge() {
        viewModelScope.launch {
            activeTransport.disconnect()
        }
    }

    fun scanTargetPhone() {
        runBromHandshake()
    }

    private fun listenToActiveTransport() {
        viewModelScope.launch {
            activeTransport.connectionState.collectLatest { state ->
                _bridgeState.value = state
                _bridgeStatus.value = _bridgeStatus.value.copy(
                    isConnected = state is BridgeConnectionState.Connected,
                    transportType = _selectedTransportType.value
                )
            }
        }
    }

    fun setScatterPlatform(chipName: String) {
        _scatterPlatform.value = chipName
        val preset = ScatterParser.getDefaultPreset(chipName)
        _partitions.value = preset.second
        addLog(TerminalLog(now(), "Loaded CPU Architecture: $chipName (Universal Built-in DA assigned)", LogLevel.INFO))
        protocolEngine.printGptAddresses(preset.second)
    }

    fun loadScatterContent(content: String, sourceFileName: String) {
        val parsed = ScatterParser.parseScatter(content)
        _scatterPlatform.value = parsed.first
        _partitions.value = parsed.second
        scatterPath.value = sourceFileName
        addLog(TerminalLog(now(), "Successfully loaded scatter: $sourceFileName (${parsed.first})", LogLevel.SUCCESS))
        addLog(TerminalLog(now(), "Found ${parsed.second.size} partitions in scatter file.", LogLevel.INFO))
        protocolEngine.printGptAddresses(parsed.second)
    }

    fun togglePartitionSelection(index: Int, isSelected: Boolean = true) {
        val list = _partitions.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isSelectedForFlashing = isSelected)
            _partitions.value = list
        }
    }

    fun selectPartition(index: Int) {
        selectPartitionIndex(index)
    }

    fun selectAllPartitions(selectAll: Boolean) {
        val list = _partitions.value.map { it.copy(isSelectedForFlashing = selectAll) }
        _partitions.value = list
        addLog(TerminalLog(now(), if (selectAll) "Selected all partitions for flashing." else "Deselected all partitions.", LogLevel.INFO))
    }

    fun toggleSelectAllPartitions(selectAll: Boolean) {
        selectAllPartitions(selectAll)
    }

    fun selectPartitionIndex(index: Int) {
        if (index in _partitions.value.indices) {
            _selectedPartitionIndex.value = index
        }
    }

    fun selectServiceFunction(func: ServiceFunction) {
        _selectedServiceFunction.value = func
        addLog(TerminalLog(now(), "Selected service function: ${func.title}", LogLevel.INFO))
    }

    fun toggleDryRun(enabled: Boolean) {
        _isDryRun.value = enabled
        if (enabled) {
            setTransportType(TransportType.SIMULATION)
            addLog(TerminalLog(now(), "Dry-Run / Simulation Mode ENABLED. Safe testing active.", LogLevel.SUCCESS))
        } else {
            addLog(TerminalLog(now(), "Dry-Run Mode DISABLED. Real hardware I/O active.", LogLevel.WARNING))
        }
    }

    fun executeActiveServiceFunction() {
        viewModelScope.launch {
            val func = _selectedServiceFunction.value
            val isSim = _isDryRun.value
            val chip = _scatterPlatform.value
            val parts = _partitions.value
            val autoReboot = _autoReboot.value
            val autoNvBackup = _autoNvBackup.value

            when (func) {
                ServiceFunction.READ_INFO -> {
                    runBromHandshake()
                }
                ServiceFunction.WRITE_PARTITION -> {
                    val part = parts.getOrNull(_selectedPartitionIndex.value)
                    if (part != null) {
                        protocolEngine.writePartition(part, null, isSim, autoNvBackup, autoReboot)
                    } else {
                        addLog(TerminalLog(now(), "Please select a valid partition to write.", LogLevel.ERROR))
                    }
                }
                ServiceFunction.BATCH_FLASH -> {
                    protocolEngine.batchFlash(chip, parts, isSim, autoNvBackup, autoReboot)
                }
                ServiceFunction.READ_PARTITION -> {
                    val part = parts.getOrNull(_selectedPartitionIndex.value)
                    if (part != null) {
                        protocolEngine.readPartition(part, isSim)
                    } else {
                        addLog(TerminalLog(now(), "Please select a valid partition to read.", LogLevel.ERROR))
                    }
                }
                ServiceFunction.DUMP_ALL_PARTITIONS -> {
                    protocolEngine.dumpAllPartitions(parts, isSim)
                }
                ServiceFunction.READ_PRELOADER -> {
                    protocolEngine.readPreloader(isSim)
                }
                ServiceFunction.READ_GPT_SCATTER -> {
                    protocolEngine.readGptAndGenerateScatter(chip, parts, isSim)
                }
                ServiceFunction.READ_RPMB -> {
                    protocolEngine.readRpmb(isSim)
                }
                ServiceFunction.BACKUP_NVRAM -> {
                    protocolEngine.backupNvram(chip, parts, isSim)
                }
                ServiceFunction.RESTORE_NVRAM -> {
                    addLog(TerminalLog(now(), "Restoring saved NV calibration archive...", LogLevel.INFO))
                    val nvPart = parts.find { it.partitionName.lowercase() == "nvdata" } ?: parts.getOrNull(2)
                    if (nvPart != null) {
                        protocolEngine.writePartition(nvPart, null, isSim, autoNvBackup = false, autoReboot = autoReboot)
                    }
                }
                ServiceFunction.BYPASS_AUTH -> {
                    protocolEngine.bypassAuth(isSim)
                }
                ServiceFunction.UNLOCK_BOOTLOADER -> {
                    protocolEngine.unlockBootloader(isSim, autoReboot)
                }
                ServiceFunction.LOCK_BOOTLOADER -> {
                    protocolEngine.lockBootloader(isSim, autoReboot)
                }
                ServiceFunction.ERASE_FRP -> {
                    protocolEngine.eraseFrp(chip, parts, isSim, autoNvBackup, autoReboot)
                }
                ServiceFunction.FACTORY_RESET -> {
                    protocolEngine.factoryReset(chip, parts, isSim, autoNvBackup, autoReboot)
                }
                ServiceFunction.FORMAT_PARTITION -> {
                    val part = parts.getOrNull(_selectedPartitionIndex.value)
                    if (part != null) {
                        protocolEngine.formatPartition(chip, part, parts, isSim, autoNvBackup, autoReboot)
                    }
                }
                ServiceFunction.CRASH_TO_BROM -> {
                    protocolEngine.crashToBrom(isSim)
                }
                ServiceFunction.REBOOT_SYSTEM -> {
                    protocolEngine.rebootDevice("Android System", isSim)
                }
                ServiceFunction.REBOOT_FASTBOOT -> {
                    protocolEngine.rebootDevice("Fastboot Mode", isSim)
                }
                ServiceFunction.REBOOT_RECOVERY -> {
                    protocolEngine.rebootDevice("Recovery Mode", isSim)
                }
                ServiceFunction.TRIGGER_TESTPOINT -> {
                    pulseTestPoint()
                }
            }
        }
    }

    fun batchFlashSelectedPartitions() {
        viewModelScope.launch {
            protocolEngine.batchFlash(_scatterPlatform.value, _partitions.value, _isDryRun.value, _autoNvBackup.value, _autoReboot.value)
        }
    }

    fun runBromHandshake() {
        viewModelScope.launch {
            val result = protocolEngine.executeBromHandshake(_isDryRun.value)
            if (result.isSuccess) {
                val info = result.getOrNull()!!
                _chipInfo.value = info
                protocolEngine.validateChipMatch(info, _scatterPlatform.value)
            }
        }
    }

    fun pulseTestPoint() {
        viewModelScope.launch {
            addLog(TerminalLog(now(), "Sending Pulse command to ESP32-S3 Hardware Bridge...", LogLevel.INFO))
            val success = activeTransport.startTrigger(
                TriggerConfig(
                    durationMs = triggerDurationMs.value,
                    pulseCount = 1,
                    activeLow = true
                )
            )
            if (success) {
                addLog(TerminalLog(now(), "ESP32-S3 test-point pulse sent (${triggerDurationMs.value}ms).", LogLevel.SUCCESS))
            } else {
                addLog(TerminalLog(now(), "Failed to send pulse command to ESP32-S3.", LogLevel.ERROR))
            }
        }
    }

    fun requestAiDiagnosis() {
        viewModelScope.launch {
            _isAiLoading.value = true
            addLog(TerminalLog(now(), "Requesting Gemini AI Diagnostic Analysis...", LogLevel.INFO))
            val recentLogs = _logs.value.takeLast(20).joinToString("\n") { "[${it.timestamp}] ${it.message}" }
            val selectedPart = _partitions.value.getOrNull(_selectedPartitionIndex.value)?.partitionName ?: "nvram"
            val diagnosis = aiAdvisor.analyzeMtkLogsAndSuggestFix(
                chipInfo = _chipInfo.value.chipIdHex,
                scatterPlatform = _scatterPlatform.value,
                recentLogs = recentLogs,
                selectedPartition = selectedPart
            )
            _aiAnalysis.value = diagnosis
            _isAiLoading.value = false
            addLog(TerminalLog(now(), "Gemini AI Diagnosis received.", LogLevel.AI))
        }
    }

    fun requestAiDiagnostics() {
        requestAiDiagnosis()
    }

    fun dismissAiSheet() {
        _aiAnalysis.value = null
    }
}
