package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppNavDestination(
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val icon: ImageVector,
    val tabTitle: String = title
) {
    FLASH(
        title = "⚡ Flash Tool",
        shortTitle = "Flash",
        subtitle = "Firmware Flashing Engine & Scatter Loader",
        icon = Icons.Default.FlashOn,
        tabTitle = "⚡ Flash (ROM)"
    ),
    BACKUP(
        title = "💾 Backup / Read",
        shortTitle = "Backup",
        subtitle = "Full ROM, Stable FW, NV & Custom Dump",
        icon = Icons.Default.FolderOpen,
        tabTitle = "💾 Backup (Dump)"
    ),
    SERVICE(
        title = "🔓 BROM Service",
        shortTitle = "BROM",
        subtitle = "One-Click FRP, Factory Reset & BL Unlock",
        icon = Icons.Default.LockOpen,
        tabTitle = "🔓 Service (BROM)"
    ),
    FASTBOOT(
        title = "🟡 Fastboot",
        shortTitle = "Fastboot",
        subtitle = "Getvar, BL Unlock, Format & Erase FRP",
        icon = Icons.Default.FlashOn,
        tabTitle = "⚡ Fastboot"
    ),
    ADB(
        title = "🔷 ADB Mode",
        shortTitle = "ADB",
        subtitle = "Device Info, Reboot to BROM/FB & Bloatware Remover",
        icon = Icons.Default.PhoneAndroid,
        tabTitle = "📱 ADB Mode"
    ),
    OTHER(
        title = "⚙️ Advanced Tools",
        shortTitle = "Advanced",
        subtitle = "Memory Test, Auth Bypass & BROM Exploits",
        icon = Icons.Default.Build,
        tabTitle = "🛠️ Advanced"
    )
}
