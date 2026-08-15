package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BridgeStatus
import com.example.model.OperationalRole
import com.example.model.TransportType
import com.example.transport.BridgeConnectionState
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun BridgeTransportCard(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTransport by viewModel.selectedTransportType.collectAsState()
    val bridgeState by viewModel.bridgeState.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val triggerDuration by viewModel.triggerDurationMs.collectAsState()
    val wifiIp by viewModel.wifiIpAddress.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SettingsInputAntenna,
                        contentDescription = "ESP32 Transport",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. ESP32-S3 Bridge Transport",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Dry-Run",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isDryRun,
                        onCheckedChange = { viewModel.toggleDryRun(it) }
                    )
                }
            }

            // Transport Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTransport == TransportType.USB_CDC,
                    onClick = { viewModel.setTransportType(TransportType.USB_CDC) },
                    label = { Text("USB-OTG Serial") },
                    leadingIcon = { Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedTransport == TransportType.WIFI_SOFTAP,
                    onClick = { viewModel.setTransportType(TransportType.WIFI_SOFTAP) },
                    label = { Text("Wi-Fi SoftAP") },
                    leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedTransport == TransportType.SIMULATION,
                    onClick = { viewModel.setTransportType(TransportType.SIMULATION) },
                    label = { Text("Simulation") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Transport Options Row
            if (selectedTransport == TransportType.WIFI_SOFTAP) {
                OutlinedTextField(
                    value = wifiIp,
                    onValueChange = { viewModel.wifiIpAddress.value = it },
                    label = { Text("ESP32-S3 IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bridgeState is BridgeConnectionState.Connected) {
                    OutlinedButton(
                        onClick = { viewModel.disconnectBridge() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = { viewModel.connectBridge() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Bridge")
                    }
                }

                // Test-Point Trigger Button
                Button(
                    onClick = { viewModel.pulseTestPoint() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pulse Test-Point", color = Color.White)
                }
            }

            // Architecture Note banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode A (Default): ESP32 shorts test-point to GND; target phone connects to Android via separate USB-OTG port.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
