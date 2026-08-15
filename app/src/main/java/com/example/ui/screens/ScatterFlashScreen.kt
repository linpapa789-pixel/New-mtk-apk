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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.PartitionEntry
import com.example.model.ServiceFunction
import com.example.parser.ScatterParser
import com.example.ui.theme.MtkBorderLight
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun ScatterFlashScreen(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val partitions by viewModel.partitions.collectAsState()
    val selectedIndex by viewModel.selectedPartitionIndex.collectAsState()
    val scatterPlatform by viewModel.scatterPlatform.collectAsState()
    val scatterPath by viewModel.scatterPath.collectAsState()
    val autoNvBackup by viewModel.autoNvBackup.collectAsState()

    var filterCategory by remember { mutableStateOf("All") }

    val filteredPartitions = remember(partitions, filterCategory) {
        when (filterCategory) {
            "Boot / Kernel" -> partitions.filter { it.partitionName in listOf("boot", "dtbo", "vbmeta", "recovery", "lk", "lk2", "tee1", "tee2") }
            "System / OS" -> partitions.filter { it.partitionName in listOf("super", "system", "vendor", "product", "userdata", "cache") }
            "NVRAM / IMEI" -> partitions.filter { it.isProtectedNv || it.partitionName in listOf("nvram", "nvdata", "nvcfg", "protect1", "protect2", "proinfo", "sec1", "seccfg") }
            "Protected" -> partitions.filter { it.isProtectedNv }
            else -> partitions
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scatter File Loader Header Card
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
                    .padding(16.dp),
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataObject,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Scatter Partition Table",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Platform: $scatterPlatform (${partitions.size} partitions)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val preset = ScatterParser.getDefaultPreset("MT6768")
                            val content = buildString {
                                appendLine("platform: ${preset.first}")
                                preset.second.forEach { p ->
                                    appendLine("- partition_index: ${p.partitionIndex}")
                                    appendLine("  partition_name: ${p.partitionName}")
                                    appendLine("  file_name: ${p.fileName}")
                                    appendLine("  linear_start_addr: ${p.linearStartAddrHex}")
                                    appendLine("  physical_start_addr: ${p.physicalStartAddrHex}")
                                    appendLine("  partition_size: ${p.partitionSizeHex}")
                                    appendLine("  region: ${p.region}")
                                    appendLine("  is_download: ${p.isDownload}")
                                }
                            }
                            viewModel.loadScatterContent(content, "MT6768_Android_scatter.txt")
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Scatter", fontSize = 12.sp)
                    }
                }
            }
        }

        // Category Filter Scroll Box
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Filter Partitions by Category:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val categories = listOf("All", "Boot / Kernel", "System / OS", "NVRAM / IMEI", "Protected")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = filterCategory == cat
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF1D4ED8) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF1D4ED8) else MtkBorderLight
                        ),
                        modifier = Modifier.clickable { filterCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Partition Table Box in Clean White Style
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MtkBorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (filteredPartitions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No partitions match filter", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(filteredPartitions) { index, part ->
                        val isSelected = selectedIndex == part.partitionIndex
                        CleanPartitionItemRow(
                            partition = part,
                            isSelected = isSelected,
                            onSelect = { viewModel.selectPartition(part.partitionIndex) }
                        )
                    }
                }
            }
        }

        // Bottom Action Bar: Flash Controls
        val activePartition = partitions.getOrNull(selectedIndex)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto NV Data Backup Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (autoNvBackup) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
                        .clickable { viewModel.toggleAutoNvBackup(!autoNvBackup) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = autoNvBackup,
                        onCheckedChange = { viewModel.toggleAutoNvBackup(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2563EB),
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Auto NV Data Backup (IMEI / Baseband)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoNvBackup) Color(0xFF1D4ED8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (autoNvBackup) "GUARD ON" else "OFF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoNvBackup) Color(0xFF15803D) else Color(0xFF94A3B8)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target: ${activePartition?.partitionName ?: "None selected"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (autoNvBackup) "Auto NV backup & SHA-256 enabled" else "Direct Flash (NV backup skipped)",
                            fontSize = 10.sp,
                            color = if (autoNvBackup) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.selectServiceFunction(ServiceFunction.WRITE_PARTITION)
                            viewModel.executeActiveServiceFunction()
                        },
                        enabled = activePartition != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flash Partition", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanPartitionItemRow(
    partition: PartitionEntry,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFFAFAFA),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF1D4ED8) else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[${partition.partitionIndex}] ${partition.partitionName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                    )
                    if (partition.isProtectedNv) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protected",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = "Offset: ${partition.linearStartAddrHex} | Size: ${partition.partitionSizeHex}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
            }

            // File binding badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (partition.fileName.isNotEmpty() && partition.fileName != "NONE") Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
            ) {
                Text(
                    text = if (partition.fileName.isNotEmpty() && partition.fileName != "NONE") partition.fileName else "No File",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (partition.fileName.isNotEmpty() && partition.fileName != "NONE") Color(0xFF15803D) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
