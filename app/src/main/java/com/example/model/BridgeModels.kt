package com.example.model

enum class TransportType(val displayName: String) {
    USB_CDC("USB-CDC Serial"),
    WIFI_SOFTAP("Wi-Fi SoftAP (WebSocket)"),
    SIMULATION("Dry-Run / Simulation")
}

enum class OperationalRole(val displayName: String, val description: String) {
    TEST_POINT_TRIGGER(
        "Mode A: Test-Point Trigger",
        "ESP32 pulses test point to force BROM mode. Phone connects directly via USB-OTG."
    ),
    UART_BRIDGE(
        "Mode B: UART Passthrough",
        "ESP32 acts as a transparent low-latency UART bridge between phone and app."
    )
}

data class BridgeStatus(
    val isConnected: Boolean = false,
    val transportType: TransportType = TransportType.SIMULATION,
    val deviceName: String = "ESP32-S3-Bridge",
    val firmwareVersion: String = "1.0.0",
    val uptimeSec: Long = 0,
    val roleMode: OperationalRole = OperationalRole.TEST_POINT_TRIGGER,
    val triggerActive: Boolean = false,
    val uartBridgeActive: Boolean = false,
    val activeBaud: Long = 115200,
    val activeClients: Int = 1,
    val ipAddress: String = "192.168.4.1"
)

data class TriggerConfig(
    val durationMs: Int = 500,
    val pulseCount: Int = 1,
    val activeLow: Boolean = true,
    val useRelay: Boolean = false
)

data class UartConfig(
    val baudRate: Long = 115200,
    val dataBits: Int = 8,
    val parity: Int = 0, // 0=None, 1=Odd, 2=Even
    val stopBits: Int = 1
)

data class MtkChipInfo(
    val chipIdHex: String = "MT6765 (0x0766)",
    val hwCodeHex: String = "0x0766",
    val hwSubcodeHex: String = "0x8A00",
    val hwVersionHex: String = "0xCA00",
    val swVersionHex: String = "0x0000",
    val secureBootEnabled: Boolean = false,
    val daLoaded: Boolean = false,
    val bromState: String = "READY"
)

data class PartitionEntry(
    val partitionIndex: Int,
    val partitionName: String,
    val fileName: String,
    val linearStartAddrHex: String,
    val physicalStartAddrHex: String,
    val partitionSizeHex: String,
    val sizeBytes: Long,
    val region: String = "EMMC_USER",
    val isDownload: Boolean = true,
    val isProtectedNv: Boolean = false,
    val isSelectedForFlashing: Boolean = true,
    val boundFilePath: String = ""
)

enum class ServiceFunction(val title: String, val subtitle: String, val isWrite: Boolean) {
    READ_INFO("Read Chip Info", "Detect MediaTek chipset, HW code, registers & security state", false),
    WRITE_PARTITION("Write / Flash Selected Partition", "Flash partition image (Mandatory Auto-Backup & SHA-256 verify)", true),
    BATCH_FLASH("Batch Flash (All Selected)", "Flash all checked partitions in sequence", true),
    READ_PARTITION("Read / Dump Selected Partition", "Dump single partition from device to local storage", false),
    DUMP_ALL_PARTITIONS("Full ROM Dump (All Partitions)", "Dump entire flash memory partitions to storage archive", false),
    READ_PRELOADER("Read Preloader / Bootloader", "Dump preloader.bin and lk bootloader images", false),
    READ_GPT_SCATTER("Read GPT & Generate Scatter", "Query GPT partition table and generate scatter.txt", false),
    READ_RPMB("Read RPMB Partition", "Dump RPMB keys and security region", false),
    BACKUP_NVRAM("Backup NVRAM / NVDATA", "Safely dump nvram, nvdata, protect1, protect2, secro, and nvcfg", false),
    RESTORE_NVRAM("Restore NVRAM / NVDATA", "Write back saved NV calibration archive with verification", true),
    BYPASS_AUTH("Bypass SLA / DAA / SBC Auth", "Inject BROM payload exploit to disable SLA/DAA security", true),
    UNLOCK_BOOTLOADER("Unlock Bootloader (seccfg)", "Write unlock payload to seccfg partition", true),
    LOCK_BOOTLOADER("Lock Bootloader (seccfg)", "Relock bootloader security state", true),
    ERASE_FRP("Erase FRP (Google Account)", "Zero-out FRP partition to remove FRP lock", true),
    FACTORY_RESET("Factory Reset (Wipe Userdata)", "Wipe userdata, metadata, and cache partitions", true),
    FORMAT_PARTITION("Format Partition", "Erase selected partition range (wipe to zeros)", true),
    CRASH_TO_BROM("Crash Preloader to BROM", "Send crash command to force preloader into BROM mode", false),
    REBOOT_SYSTEM("Reboot to System", "Send DA reboot command to boot Android OS", false),
    REBOOT_FASTBOOT("Reboot to Bootloader (Fastboot)", "Send DA reboot command to enter fastboot mode", false),
    REBOOT_RECOVERY("Reboot to Recovery", "Send DA reboot command to enter recovery mode", false),
    TRIGGER_TESTPOINT("Pulse Test-Point", "Fire ESP32 GPIO/relay pulse to force BROM mode", false)
}

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR, RAW, AI
}

data class TerminalLog(
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

data class OperationProgress(
    val isRunning: Boolean = false,
    val title: String = "",
    val detail: String = "",
    val percentage: Float = 0f,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val speedKbPerSec: Double = 0.0,
    val estimatedSecondsRemaining: Int = 0
)
