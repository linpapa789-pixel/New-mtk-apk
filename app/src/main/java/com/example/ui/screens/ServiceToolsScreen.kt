package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServiceFunction
import com.example.ui.theme.MtkBorderLight
import com.example.ui.theme.StatusError
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun ServiceToolsScreen(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val selectedFunction by viewModel.selectedServiceFunction.collectAsState()
    val progress by viewModel.operationProgress.collectAsState()
    val partitions by viewModel.partitions.collectAsState()
    val selectedPartIndex by viewModel.selectedPartitionIndex.collectAsState()
    val selectedPartition = partitions.getOrNull(selectedPartIndex)
    val isDryRun by viewModel.isDryRun.collectAsState()

    var showWriteConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NVRAM / IMEI Safe-Backup Hero Card
        NvramProtectionHeroCard(
            onBackupNvram = {
                viewModel.selectServiceFunction(ServiceFunction.BACKUP_NVRAM)
                viewModel.executeActiveServiceFunction()
            }
        )

        // Service Functions Horizontal Scroll Box
        ServiceFunctionScrollBox(
            selectedFunction = selectedFunction,
            onSelectFunction = { viewModel.selectServiceFunction(it) }
        )

        // Active Function Configuration & Execution Card (Pure White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MtkBorderLight),
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
                                imageVector = when (selectedFunction) {
                                    ServiceFunction.READ_INFO -> Icons.Default.Fingerprint
                                    ServiceFunction.TRIGGER_TESTPOINT -> Icons.Default.Bolt
                                    ServiceFunction.BACKUP_NVRAM -> Icons.Default.Security
                                    ServiceFunction.RESTORE_NVRAM -> Icons.Default.Restore
                                    ServiceFunction.READ_PARTITION -> Icons.Default.Download
                                    ServiceFunction.WRITE_PARTITION -> Icons.Default.Upload
                                    ServiceFunction.FORMAT_PARTITION -> Icons.Default.DeleteForever
                                    else -> Icons.Default.PlayArrow
                                },
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = selectedFunction.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedFunction.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Partition Target if relevant
                if (selectedPartition != null && (selectedFunction == ServiceFunction.READ_PARTITION || selectedFunction == ServiceFunction.WRITE_PARTITION || selectedFunction == ServiceFunction.FORMAT_PARTITION)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Target Partition Selected:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "[${selectedPartition.partitionIndex}] ${selectedPartition.partitionName} (${selectedPartition.partitionSizeHex})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }
                }

                // Progress Bar if running
                if (progress.isRunning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = progress.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.1f", progress.percentage)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF1D4ED8),
                            trackColor = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = progress.detail,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Execute Button
                val isDestructive = selectedFunction in listOf(
                    ServiceFunction.WRITE_PARTITION,
                    ServiceFunction.FORMAT_PARTITION,
                    ServiceFunction.RESTORE_NVRAM
                )

                Button(
                    onClick = {
                        if (isDestructive && !isDryRun) {
                            showWriteConfirmationDialog = true
                        } else {
                            viewModel.executeActiveServiceFunction()
                        }
                    },
                    enabled = !progress.isRunning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) Color(0xFFDC2626) else Color(0xFF1D4ED8)
                    )
                ) {
                    Icon(
                        imageVector = if (isDestructive) Icons.Default.Warning else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDestructive) "Execute ${selectedFunction.title} (Safety Alert)" else "Execute ${selectedFunction.title}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Safety Confirmation Dialog
    if (showWriteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmationDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError) },
            title = { Text("Confirm Operation") },
            text = {
                Text(
                    "You are about to execute '${selectedFunction.title}' on target device flash memory.\n\n" +
                            "• A safety backup will be generated automatically.\n" +
                            "• Do not unplug USB cables during execution.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWriteConfirmationDialog = false
                        viewModel.executeActiveServiceFunction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Proceed with Operation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NvramProtectionHeroCard(
    onBackupNvram: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NVRAM & IMEI Protection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14532D)
                    )
                    Text(
                        text = "Backup nvram, nvdata, nvcfg & protect",
                        fontSize = 11.sp,
                        color = Color(0xFF166534)
                    )
                }
            }

            Button(
                onClick = onBackupNvram,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ServiceFunctionScrollBox(
    selectedFunction: ServiceFunction,
    onSelectFunction: (ServiceFunction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Select Service Operation:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val functions = listOf(
            ServiceFunction.READ_INFO,
            ServiceFunction.TRIGGER_TESTPOINT,
            ServiceFunction.BACKUP_NVRAM,
            ServiceFunction.RESTORE_NVRAM,
            ServiceFunction.READ_PARTITION,
            ServiceFunction.WRITE_PARTITION,
            ServiceFunction.FORMAT_PARTITION
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(functions) { fn ->
                val isSelected = selectedFunction == fn
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF1D4ED8) else MtkBorderLight
                    ),
                    modifier = Modifier.clickable { onSelectFunction(fn) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (fn) {
                                ServiceFunction.READ_INFO -> Icons.Default.Fingerprint
                                ServiceFunction.TRIGGER_TESTPOINT -> Icons.Default.Bolt
                                ServiceFunction.BACKUP_NVRAM -> Icons.Default.Security
                                ServiceFunction.RESTORE_NVRAM -> Icons.Default.Restore
                                ServiceFunction.READ_PARTITION -> Icons.Default.Download
                                ServiceFunction.WRITE_PARTITION -> Icons.Default.Upload
                                ServiceFunction.FORMAT_PARTITION -> Icons.Default.DeleteForever
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = fn.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}
