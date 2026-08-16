package com.example.model

enum class TransportType(val displayName: String) {
    USB_OTG_DIRECT("Direct USB OTG (Host)"),
    SIMULATION("Dry-Run / Simulation")
}

data class BridgeStatus(
    val isConnected: Boolean = false,
    val transportType: TransportType = TransportType.USB_OTG_DIRECT,
    val deviceName: String = "MTK Direct USB Host",
    val fileDescriptor: Int = -1,
    val isBromMode: Boolean = true,
    val targetVidPid: String = "0x0E8D:0x0003",
    val endpointIn: Int = 0x81,
    val endpointOut: Int = 0x01
)

data class TriggerConfig(
    val durationMs: Int = 100,
    val pulseCount: Int = 1,
    val activeLow: Boolean = true
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
    READ_INFO("Read Chip Info", "Detect MediaTek chipset, HW code, registers & security state via USB", false),
    WRITE_PARTITION("Write / Flash Selected Partition", "Flash partition image (Mandatory Auto-Backup & SHA-256 verify)", true),
    BATCH_FLASH("Batch Flash (All Selected)", "Flash all checked partitions in sequence via USB OTG", true),
    READ_PARTITION("Read / Dump Selected Partition", "Dump single partition from device to local storage", false),
    DUMP_ALL_PARTITIONS("Full ROM Dump (All Partitions)", "Dump entire flash memory partitions to storage archive", false),
    READ_PRELOADER("Read Preloader / Bootloader", "Dump preloader.bin and lk bootloader images", false),
    READ_GPT_SCATTER("Read GPT & Generate Scatter", "Query GPT partition table and generate scatter.txt", false),
    READ_RPMB("Read RPMB Partition", "Dump RPMB keys and security region", false),
    BACKUP_NVRAM("Backup NVRAM / NVDATA", "Safely dump nvram, nvdata, protect1, protect2, secro, and nvcfg", false),
    RESTORE_NVRAM("Restore NVRAM / NVDATA", "Write back saved NV calibration archive with verification", true),
    BYPASS_AUTH("Bypass SLA / DAA / SBC Auth", "Execute USB Control Transfer exploit to disable SLA/DAA security", true),
    UNLOCK_BOOTLOADER("Unlock Bootloader (seccfg)", "Write unlock payload to seccfg partition", true),
    LOCK_BOOTLOADER("Lock Bootloader (seccfg)", "Relock bootloader security state", true),
    ERASE_FRP("Erase FRP (Google Account)", "Zero-out FRP partition to remove FRP lock", true),
    FACTORY_RESET("Factory Reset (Wipe Userdata)", "Wipe userdata, metadata, and cache partitions", true),
    FORMAT_PARTITION("Format Partition", "Erase selected partition range (wipe to zeros)", true),
    CRASH_TO_BROM("Crash Preloader to BROM", "Send USB Control Transfer command to force preloader into BROM", false),
    REBOOT_SYSTEM("Reboot to System", "Send DA reboot command to boot Android OS", false),
    REBOOT_FASTBOOT("Reboot to Bootloader (Fastboot)", "Send DA reboot command to enter fastboot mode", false),
    REBOOT_RECOVERY("Reboot to Recovery", "Send DA reboot command to enter recovery mode", false)
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
