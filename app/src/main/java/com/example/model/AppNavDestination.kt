package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Usb
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppNavDestination(
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val icon: ImageVector
) {
    UNLOCKTOOL_CONSOLE(
        title = "MTK Flashing & Service Console",
        shortTitle = "Flasher",
        subtitle = "Direct USB OTG Scatter Flash, FRP & NVRAM",
        icon = Icons.Default.FlashOn
    )
}
