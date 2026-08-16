package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppNavDestination(
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val icon: ImageVector
) {
    FLASH(
        title = "⚡ Flash Tool",
        shortTitle = "Flash",
        subtitle = "Firmware Flashing Engine & Scatter Loader",
        icon = Icons.Default.FlashOn
    ),
    BACKUP(
        title = "💾 Backup / Read",
        shortTitle = "Backup",
        subtitle = "Full ROM, Stable FW, NV & Custom Dump",
        icon = Icons.Default.FolderOpen
    ),
    SERVICE(
        title = "🔓 Service / Unlock",
        shortTitle = "Service",
        subtitle = "One-Click FRP, Factory Reset & BL Unlock",
        icon = Icons.Default.LockOpen
    ),
    OTHER(
        title = "⚙️ Other & Advanced",
        shortTitle = "Other",
        subtitle = "Memory Test, Auth Bypass & Gemini AI",
        icon = Icons.Default.AutoAwesome
    )
}

