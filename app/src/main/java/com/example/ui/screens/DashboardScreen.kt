package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNavDestination
import com.example.model.BridgeStatus
import com.example.model.MtkChipInfo
import com.example.protocol.TargetPhoneState
import com.example.transport.BridgeConnectionState
import com.example.ui.theme.MtkBorderLight
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun DashboardScreen(
    viewModel: MtkBridgeViewModel,
    onNavigate: (AppNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val bridgeState by viewModel.bridgeState.collectAsState()
    val targetPhoneState by viewModel.targetPhoneState.collectAsState()
    val chipInfo by viewModel.chipInfo.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Actions Horizontal Scroll Box
        ScrollableQuickActionBox(
            onNavigate = onNavigate,
            onPulseTestPoint = { viewModel.pulseTestPoint() },
            onHandshake = { viewModel.runBromHandshake() },
            onScanPhone = { viewModel.scanTargetPhone() }
        )

        // Target Device Connection Card (Primary Hero)
        TargetDeviceBROMCard(
            viewModel = viewModel,
            phoneState = targetPhoneState,
            chipInfo = chipInfo,
            isDryRun = isDryRun
        )

        // Hardware Chipset & Security Specifications Card
        ChipsetDetailsCard(
            chipInfo = chipInfo,
            phoneState = targetPhoneState,
            onRefresh = { viewModel.runBromHandshake() }
        )

        // ESP32 Bridge Quick Link Summary Card
        Esp32QuickSummaryCard(
            bridgeState = bridgeState,
            bridgeStatus = bridgeStatus,
            onConfigureBridge = { onNavigate(AppNavDestination.SETTINGS) },
            onPulseTestPoint = { viewModel.pulseTestPoint() }
        )
    }
}

@Composable
private fun ScrollableQuickActionBox(
    onNavigate: (AppNavDestination) -> Unit,
    onPulseTestPoint: () -> Unit,
    onHandshake: () -> Unit,
    onScanPhone: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Quick Tools & Shortcuts",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                QuickActionChip(
                    icon = Icons.Default.Cached,
                    label = "BROM Handshake",
                    accentColor = Color(0xFF1D4ED8),
                    onClick = onHandshake
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Bolt,
                    label = "Test-Point Pulse",
                    accentColor = Color(0xFF0284C7),
                    onClick = onPulseTestPoint
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Search,
                    label = "Scan USB Host",
                    accentColor = Color(0xFF4F46E5),
                    onClick = onScanPhone
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Security,
                    label = "NVRAM Backup",
                    accentColor = Color(0xFF10B981),
                    onClick = { onNavigate(AppNavDestination.NV_BACKUP_RESTORE) }
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.FlashOn,
                    label = "Scatter Flasher",
                    accentColor = Color(0xFFF59E0B),
                    onClick = { onNavigate(AppNavDestination.PARTITION_FLASH) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MtkBorderLight),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TargetDeviceBROMCard(
    viewModel: MtkBridgeViewModel,
    phoneState: TargetPhoneState,
    chipInfo: MtkChipInfo,
    isDryRun: Boolean
) {
    val isConnected = phoneState is TargetPhoneState.Connected || isDryRun
    val isBrom = if (phoneState is TargetPhoneState.Connected) phoneState.isBromMode else true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MtkBorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Target Phone",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Target Device Handshake",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MediaTek BootROM USB Protocol",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Connection badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isConnected) StatusSuccess.copy(alpha = 0.12f) else StatusWarning.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) StatusSuccess else StatusWarning)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) (if (isBrom) "BROM ACTIVE" else "PRELOADER") else "DISCONNECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) StatusSuccess else StatusWarning
                        )
                    }
                }
            }

            // Connection Status Message Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Usb,
                        contentDescription = null,
                        tint = if (isConnected) StatusSuccess else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isConnected) "Device Synced (${chipInfo.chipIdHex})" else "Waiting for MTK Phone USB Connection",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isConnected) "BROM sync 0xA0 0x0A 0x50 0x05 passed. Ready for flash operations." else "Connect target phone via USB-OTG and hold Vol Down or short test-point to GND.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Handshake Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.runBromHandshake() },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BROM Handshake", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.scanTargetPhone() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan USB")
                }
            }
        }
    }
}

@Composable
private fun ChipsetDetailsCard(
    chipInfo: MtkChipInfo,
    phoneState: TargetPhoneState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MtkBorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Chipset",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hardware Chipset Specs",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = chipInfo.bromState,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Specs 2x2 Grid in pure white style
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecTile(title = "Chip Identifier", value = chipInfo.chipIdHex, modifier = Modifier.weight(1f))
                    SpecTile(title = "HW Code / Subcode", value = "${chipInfo.hwCodeHex} / ${chipInfo.hwSubcodeHex}", modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecTile(title = "HW Version", value = chipInfo.hwVersionHex, modifier = Modifier.weight(1f))
                    SpecTile(title = "SW Version", value = chipInfo.swVersionHex, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecTile(title = "Secure Boot SLA", value = if (chipInfo.secureBootEnabled) "Enabled (Secure)" else "Disabled (Bypassed)", modifier = Modifier.weight(1f))
                    SpecTile(title = "DA Agent Loaded", value = if (chipInfo.daLoaded) "Loaded (High-Speed)" else "BROM Native Mode", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpecTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF0F172A),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Esp32QuickSummaryCard(
    bridgeState: BridgeConnectionState,
    bridgeStatus: BridgeStatus,
    onConfigureBridge: () -> Unit,
    onPulseTestPoint: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MtkBorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F3FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsInputAntenna,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ESP32-S3 Bridge",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Test-Point & Transport Controller",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onConfigureBridge,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Config", fontSize = 12.sp)
                }
            }
        }
    }
}
