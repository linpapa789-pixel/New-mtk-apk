package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppNavDestination(
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val icon: ImageVector
) {
    UNLOCKTOOL_CONSOLE(
        title = "UnlockTool Flashing Console",
        shortTitle = "UnlockTool",
        subtitle = "Scatter Flashing, FRP & NVRAM Service",
        icon = Icons.Default.FlashOn
    ),
    ESP32_BRIDGE(
        title = "ESP32-S3 Hardware Bridge",
        shortTitle = "Hardware Bridge",
        subtitle = "WiFi SoftAP, USB Host & Test-Point Trigger",
        icon = Icons.Default.SettingsInputAntenna
    )
}
