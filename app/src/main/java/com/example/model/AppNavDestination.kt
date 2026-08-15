package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppNavDestination(
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val icon: ImageVector
) {
    PARTITION_FLASH(
        title = "Partition Flash",
        shortTitle = "Flash Tool",
        subtitle = "Scatter Flashing & Data Grid",
        icon = Icons.Default.FlashOn
    ),
    NV_BACKUP_RESTORE(
        title = "NV Data Backup/Restore",
        shortTitle = "NVRAM / IMEI",
        subtitle = "Calibration & Security Partition Dump",
        icon = Icons.Default.Security
    ),
    READ_CHIP_INFO(
        title = "Read Chip Info",
        shortTitle = "Chip Info",
        subtitle = "MediaTek Hardware & BROM State",
        icon = Icons.Default.Memory
    ),
    SERIAL_LOG_MONITOR(
        title = "Serial / Log Monitor",
        shortTitle = "Live Log",
        subtitle = "Monospace Console & AI Advisor",
        icon = Icons.Default.Terminal
    ),
    DA_SCATTER_MANAGER(
        title = "DA / Preloader / Scatter Manager",
        shortTitle = "File Manager",
        subtitle = "Manage MTK Download Agents & Partitions",
        icon = Icons.Default.DataObject
    ),
    SETTINGS(
        title = "Settings",
        shortTitle = "Settings",
        subtitle = "ESP32 Bridge & Baud Rates",
        icon = Icons.Default.Settings
    )
}
