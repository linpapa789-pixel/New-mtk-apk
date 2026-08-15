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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.model.BridgeStatus
import com.example.model.OperationalRole
import com.example.model.TransportType
import com.example.transport.BridgeConnectionState
import com.example.ui.theme.MtkBorderLight
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun Esp32BridgeScreen(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTransport by viewModel.selectedTransportType.collectAsState()
    val bridgeState by viewModel.bridgeState.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val triggerDuration by viewModel.triggerDurationMs.collectAsState()
    val wifiIp by viewModel.wifiIpAddress.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Transport Selection Scroll Box
        TransportSelectionScrollBox(
            selectedTransport = selectedTransport,
            onSelectTransport = { viewModel.setTransportType(it) }
        )

        // Connection Configuration Card (White Card)
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
                                imageVector = Icons.Default.SettingsInputAntenna,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bridge Connection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (selectedTransport) {
                                    TransportType.USB_CDC -> "USB CDC Serial (/dev/ttyACM*)"
                                    TransportType.WIFI_SOFTAP -> "ESP32-S3-BROM-Bridge SoftAP"
                                    TransportType.SIMULATION -> "Loopback Hardware Simulation"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Connection status badge
                    val isConnected = bridgeState is BridgeConnectionState.Connected
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
                                text = if (isConnected) "ONLINE" else "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) StatusSuccess else StatusWarning
                            )
                        }
                    }
                }

                // Wi-Fi Configuration Field if WiFi selected
                if (selectedTransport == TransportType.WIFI_SOFTAP) {
                    OutlinedTextField(
                        value = wifiIp,
                        onValueChange = { viewModel.wifiIpAddress.value = it },
                        label = { Text("ESP32-S3 IP Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Connect / Disconnect Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (bridgeState is BridgeConnectionState.Connected) {
                        OutlinedButton(
                            onClick = { viewModel.disconnectBridge() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Disconnect Bridge")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.connectBridge() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                        ) {
                            Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect ESP32-S3", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Test-Point GPIO Hardware Pulse Controller Card
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
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hardware Test-Point Trigger",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "GPIO Pulse Generator (Active-Low)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Duration Presets Scroll Box
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pulse Duration Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val currentDurInt = triggerDuration
                    val presets = listOf(50, 100, 200, 300, 500)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets) { preset ->
                            val isSelected = currentDurInt == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { viewModel.triggerDurationMs.value = preset }
                            ) {
                                Text(
                                    text = "${preset}ms",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Pulse Duration Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val currentDurFloat = triggerDuration.toFloat().coerceIn(20f, 1000f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Custom Duration",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${triggerDuration} ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                    Slider(
                        value = currentDurFloat,
                        onValueChange = { viewModel.triggerDurationMs.value = it.toInt() },
                        valueRange = 20f..1000f,
                        steps = 19
                    )
                }

                // Primary Trigger Button
                Button(
                    onClick = { viewModel.pulseTestPoint() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚡ Trigger Test-Point (${triggerDuration}ms Active-Low)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Operational Mode Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode A: Hardware Test-Point Trigger",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "ESP32-S3 shorts the phone's hardware test-point pad to GND for the configured pulse duration. Target phone connects directly to Android USB-OTG port for high-speed BROM flashing.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TransportSelectionScrollBox(
    selectedTransport: TransportType,
    onSelectTransport: (TransportType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Select Hardware Transport Interface",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TransportChip(
                    title = "USB-OTG Serial",
                    icon = Icons.Default.Usb,
                    isSelected = selectedTransport == TransportType.USB_CDC,
                    onClick = { onSelectTransport(TransportType.USB_CDC) }
                )
            }
            item {
                TransportChip(
                    title = "Wi-Fi SoftAP",
                    icon = Icons.Default.Wifi,
                    isSelected = selectedTransport == TransportType.WIFI_SOFTAP,
                    onClick = { onSelectTransport(TransportType.WIFI_SOFTAP) }
                )
            }
            item {
                TransportChip(
                    title = "Dry-Run Simulation",
                    icon = Icons.Default.PlayArrow,
                    isSelected = selectedTransport == TransportType.SIMULATION,
                    onClick = { onSelectTransport(TransportType.SIMULATION) }
                )
            }
        }
    }
}

@Composable
private fun TransportChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF1D4ED8) else MtkBorderLight
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A)
            )
        }
    }
}
