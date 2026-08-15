package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
import com.example.model.PartitionEntry
import com.example.model.ServiceFunction
import com.example.model.TerminalLog
import com.example.model.TransportType
import com.example.parser.ScatterParser
import com.example.protocol.TargetPhoneState
import com.example.ui.theme.MtkBorderLight
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TerminalTimestamp
import com.example.viewmodel.MtkBridgeViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun UnlockToolFlashScreen(
    viewModel: MtkBridgeViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val partitions by viewModel.partitions.collectAsState()
    val selectedPartIndex by viewModel.selectedPartitionIndex.collectAsState()
    val selectedServiceFunction by viewModel.selectedServiceFunction.collectAsState()
    val progress by viewModel.operationProgress.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()
    val chipInfo by viewModel.chipInfo.collectAsState()
    val transportType by viewModel.selectedTransportType.collectAsState()
    val targetPhoneState by viewModel.targetPhoneState.collectAsState()
    val scatterPlatform by viewModel.scatterPlatform.collectAsState()
    val scatterPath by viewModel.scatterPath.collectAsState()
    val daPath by viewModel.daAgentPath.collectAsState()
    val preloaderPath by viewModel.preloaderPath.collectAsState()
    val autoNvBackup by viewModel.autoNvBackup.collectAsState()
    val autoReboot by viewModel.autoReboot.collectAsState()
    val backupLocation by viewModel.backupLocation.collectAsState()

    // Form Dropdown states
    var selectedChipset by remember { mutableStateOf("Auto-Detect (Helio/Dimensity)") }
    var selectedConnMode by remember { mutableStateOf("USB CDC / BROM VCOM") }
    var selectedInterface by remember { mutableStateOf("BROM USB Direct") }
    var selectedDaTimeout by remember { mutableStateOf("10s (Standard)") }
    var selectedLinkSpeed by remember { mutableStateOf("921600 Baud / High-Speed") }

    // Toggle states
    var autoDetectChipId by remember { mutableStateOf(true) }
    var verifyAfterWrite by remember { mutableStateOf(false) }
    var partitionOffsetInput by remember { mutableStateOf("0x00000000") }

    // Dropdown expansion states
    var expChipset by remember { mutableStateOf(false) }
    var expConnMode by remember { mutableStateOf(false) }
    var expInterface by remember { mutableStateOf(false) }
    var expTimeout by remember { mutableStateOf(false) }
    var expBaud by remember { mutableStateOf(false) }
    var expServiceFunc by remember { mutableStateOf(false) }

    // Log terminal list state for auto-scroll
    val logListState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    // System File Pickers for Scatter, DA, and Preloader
    val scatterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "scatter.txt"
                viewModel.loadScatterContent(content, fileName)
            } catch (e: Exception) {
                viewModel.addLog(TerminalLog(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), "Failed to read scatter file: ${e.message}", LogLevel.ERROR))
            }
        }
    }

    val daPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "MTK_AllInOne_DA.bin"
            viewModel.customDaPath.value = fileName
            viewModel.addLog(TerminalLog(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), "Selected Custom DA File: $fileName", LogLevel.INFO))
        }
    }

    val preloaderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "preloader_default.bin"
            viewModel.preloaderPath.value = fileName
            viewModel.addLog(TerminalLog(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), "Selected Preloader: $fileName", LogLevel.INFO))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Persistent Status Strip with compact navigation toggle
        PersistentStatusStrip(
            transportType = transportType,
            chipInfo = chipInfo,
            targetPhoneState = targetPhoneState,
            isDryRun = isDryRun,
            onOpenDrawer = onOpenDrawer
        )

        // Main Scrollable Workspace
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Tool Form Card (Upper Half)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MtkBorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Target Chipset (col 1) | Connection Mode (col 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolDropdown(
                            label = "Target Chipset",
                            selectedText = selectedChipset,
                            expanded = expChipset,
                            onExpandedChange = { expChipset = it },
                            items = listOf(
                                "Auto-Detect (Helio/Dimensity)",
                                "MT6765 (Helio P35 / G35)",
                                "MT6768 (Helio G80 / G85)",
                                "MT6785 (Helio G90T / G95)",
                                "MT6833 (Dimensity 700)",
                                "MT6877 (Dimensity 900)",
                                "MT6893 (Dimensity 1200)"
                            ),
                            onSelect = { selectedChipset = it },
                            modifier = Modifier.weight(1f)
                        )

                        ToolDropdown(
                            label = "Connection Mode",
                            selectedText = selectedConnMode,
                            expanded = expConnMode,
                            onExpandedChange = { expConnMode = it },
                            items = listOf(
                                "USB CDC / BROM VCOM",
                                "Wi-Fi SoftAP (ESP32-S3)",
                                "UART Passthrough Bridge"
                            ),
                            onSelect = {
                                selectedConnMode = it
                                if (it.contains("Wi-Fi")) {
                                    viewModel.setTransportType(TransportType.WIFI_SOFTAP)
                                } else {
                                    viewModel.setTransportType(TransportType.USB_CDC)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Interface (3 col) | DA Handshake Timeout | Baud/Link Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ToolDropdown(
                            label = "Interface",
                            selectedText = selectedInterface,
                            expanded = expInterface,
                            onExpandedChange = { expInterface = it },
                            items = listOf("BROM USB Direct", "UART Bootloader", "High-Speed SPI"),
                            onSelect = { selectedInterface = it },
                            modifier = Modifier.weight(1f)
                        )

                        ToolDropdown(
                            label = "DA Timeout",
                            selectedText = selectedDaTimeout,
                            expanded = expTimeout,
                            onExpandedChange = { expTimeout = it },
                            items = listOf("3s (Fast)", "5s (Standard)", "10s (Recommended)", "30s (Long)"),
                            onSelect = { selectedDaTimeout = it },
                            modifier = Modifier.weight(1f)
                        )

                        ToolDropdown(
                            label = "Baud / Link Speed",
                            selectedText = selectedLinkSpeed,
                            expanded = expBaud,
                            onExpandedChange = { expBaud = it },
                            items = listOf("921600 Baud", "460800 Baud", "115200 Baud", "USB Full-Speed"),
                            onSelect = { selectedLinkSpeed = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: Safety & Options Toggles
                    // 3a: Primary Protection Options (Auto NV Data Backup Checkbox & Auto-Reboot)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (autoNvBackup) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (autoNvBackup) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox for Auto NV Data Backup
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { viewModel.toggleAutoNvBackup(!autoNvBackup) }
                            ) {
                                Checkbox(
                                    checked = autoNvBackup,
                                    onCheckedChange = { viewModel.toggleAutoNvBackup(it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF2563EB),
                                        checkmarkColor = Color.White,
                                        uncheckedColor = Color(0xFF94A3B8)
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Auto NV Data Backup",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (autoNvBackup) Color(0xFF1D4ED8) else Color(0xFF475569)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (autoNvBackup) Color(0xFFDBEAFE) else Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = if (autoNvBackup) "IMEI Guard ON" else "OFF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (autoNvBackup) Color(0xFF1E40AF) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (autoNvBackup) "Auto-dumps nvram, nvdata & builds scatter" else "Skip NV backup (Direct Operation)",
                                        fontSize = 9.sp,
                                        color = if (autoNvBackup) Color(0xFF3B82F6) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Auto-Reboot Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Switch(
                                    checked = autoReboot,
                                    onCheckedChange = { viewModel.toggleAutoReboot(it) },
                                    modifier = Modifier.size(30.dp),
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF059669))
                                )
                                Text("Auto-Reboot", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                        }
                    }

                    // 3b: Secondary Toggles ("Auto-Detect Chip ID" | "Verify SHA-256")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Switch(
                                checked = autoDetectChipId,
                                onCheckedChange = { autoDetectChipId = it },
                                modifier = Modifier.size(30.dp),
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1D4ED8))
                            )
                            Text("Auto-Detect Chip", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Switch(
                                checked = verifyAfterWrite,
                                onCheckedChange = { verifyAfterWrite = it },
                                modifier = Modifier.size(30.dp),
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1D4ED8))
                            )
                            Text("Verify SHA-256", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }
                    }

                    // Backup Output Destination Notice Banner
                    if (autoNvBackup) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Backup & Scatter Output: $backupLocation",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF1E40AF),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Dynamic Partition Offset Text Field (revealed when Verify is enabled)
                    AnimatedVisibility(visible = verifyAfterWrite) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Verification Offset / Linear Address:", fontSize = 11.sp, color = Color(0xFF64748B))
                                OutlinedTextField(
                                    value = partitionOffsetInput,
                                    onValueChange = { partitionOffsetInput = it },
                                    modifier = Modifier.width(160.dp).height(44.dp),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1D4ED8),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Row 4: Three File-Picker Rows (Preloader, Custom DA, Scatter.txt)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilePickerRow(
                            label = "Preloader File",
                            fileName = preloaderPath.ifEmpty { "preloader_default.bin" },
                            isLoaded = preloaderPath.isNotEmpty(),
                            onPickFile = { preloaderPickerLauncher.launch("*/*") },
                            onClearFile = { viewModel.preloaderPath.value = "" }
                        )

                        FilePickerRow(
                            label = "Custom DA Agent",
                            fileName = daPath.ifEmpty { "MTK_AllInOne_DA.bin" },
                            isLoaded = daPath.isNotEmpty(),
                            onPickFile = { daPickerLauncher.launch("*/*") },
                            onClearFile = { viewModel.daAgentPath.value = "Built-in Universal DA (MTK All-in-One)" }
                        )

                        FilePickerRow(
                            label = "Scatter File",
                            fileName = scatterPath.ifEmpty { "MT6765_Android_scatter.txt (Preset Loaded)" },
                            isLoaded = scatterPath.isNotEmpty() || partitions.isNotEmpty(),
                            onPickFile = { scatterPickerLauncher.launch("*/*") },
                            onClearFile = {
                                viewModel.scatterPath.value = ""
                            }
                        )
                    }

                    // Row 5: Service / One-Click Function Dropdown
                    ToolDropdown(
                        label = "Service / One-Click Function",
                        selectedText = selectedServiceFunction.title,
                        expanded = expServiceFunc,
                        onExpandedChange = { expServiceFunc = it },
                        items = ServiceFunction.values().map { it.title },
                        onSelect = { title ->
                            ServiceFunction.values().find { it.title == title }?.let {
                                viewModel.selectServiceFunction(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Compact Partition Table Card (Unlock Tool / GSM SeaTool Style Table with Checkboxes)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MtkBorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header Bar
                    val allSelected = partitions.isNotEmpty() && partitions.all { it.isSelectedForFlashing }
                    Surface(
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { viewModel.toggleSelectAllPartitions(it) },
                                modifier = Modifier.size(20.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF1D4ED8),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("No.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.width(26.dp))
                            Text("Partition", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.width(85.dp))
                            Text("Image / File", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1f))
                            Text("Offset", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.width(80.dp))
                            Text("Size", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.width(60.dp))
                        }
                    }

                    HorizontalDivider(color = MtkBorderLight)

                    // Partition Table Rows
                    if (partitions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No scatter loaded. Choose a scatter.txt file above.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(partitions) { index, part ->
                                val isSelected = selectedPartIndex == index
                                CompactPartitionRow(
                                    partition = part,
                                    isSelected = isSelected,
                                    onCheckChange = { viewModel.togglePartitionSelection(index) },
                                    onSelect = { viewModel.selectPartition(index) }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Progress Indicator Strip (when active)
            if (progress.isRunning) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(progress.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                            Text("${String.format("%.1f", progress.percentage)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        }
                        LinearProgressIndicator(
                            progress = { progress.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF1D4ED8),
                            trackColor = Color(0xFFDBEAFE)
                        )
                    }
                }
            }

            // Bottom Action Control Row (Unlock Tool Icon Buttons + Primary Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Profile (Disk)
                IconButton(
                    onClick = { viewModel.addLog(TerminalLog(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), "Current Flash Profile saved.", LogLevel.SUCCESS)) },
                    modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, MtkBorderLight, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Profile", tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                }

                // Export Log (Share)
                IconButton(
                    onClick = {
                        val text = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                        clipboardManager.setText(AnnotatedString(text))
                        viewModel.addLog(TerminalLog(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), "Terminal logs copied to clipboard.", LogLevel.INFO))
                    },
                    modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, MtkBorderLight, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Log", tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                }

                // TP Pulse (ESP32-S3)
                OutlinedButton(
                    onClick = { viewModel.pulseTestPoint() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TP Pulse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Secondary Action: Format / Erase (Red/Orange)
                Button(
                    onClick = {
                        viewModel.selectServiceFunction(ServiceFunction.FORMAT_PARTITION)
                        viewModel.executeActiveServiceFunction()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Format", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Primary Action: START FLASH (Large Blue Button)
                Button(
                    onClick = { viewModel.executeActiveServiceFunction() },
                    enabled = !progress.isRunning,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    modifier = Modifier.height(36.dp).weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START FLASH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Monospace Live Terminal Log Box (GSM Tool Console Style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Terminal Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CONSOLE OUTPUT (${logs.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    val text = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                                    clipboardManager.setText(AnnotatedString(text))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                            }
                            IconButton(
                                onClick = { viewModel.clearLogs() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                    // Live Log Lines
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(logs) { _, log ->
                            val color = when (log.level) {
                                LogLevel.SUCCESS -> StatusSuccess
                                LogLevel.ERROR -> StatusError
                                LogLevel.WARNING -> StatusWarning
                                LogLevel.AI -> Color(0xFFC084FC)
                                LogLevel.RAW -> Color(0xFF94A3B8)
                                LogLevel.INFO -> TerminalText
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("[${log.timestamp}] ", fontSize = 10.sp, color = TerminalTimestamp, fontFamily = FontFamily.Monospace)
                                Text(log.message, fontSize = 10.sp, color = color, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Persistent Status Strip with compact Navigation & Chipset indicators
@Composable
private fun PersistentStatusStrip(
    transportType: TransportType,
    chipInfo: com.example.model.MtkChipInfo,
    targetPhoneState: TargetPhoneState,
    isDryRun: Boolean,
    onOpenDrawer: () -> Unit = {}
) {
    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = if (transportType == TransportType.WIFI_SOFTAP) Icons.Default.Wifi else Icons.Default.Usb,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = transportType.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Chip: ${chipInfo.chipIdHex}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8)
                )

                val stateText = when {
                    isDryRun -> "SIMULATION"
                    targetPhoneState is TargetPhoneState.Connected -> "BROM READY"
                    else -> "WAITING"
                }
                val stateColor = when {
                    isDryRun -> Color(0xFFFBBF24)
                    targetPhoneState is TargetPhoneState.Connected -> Color(0xFF4ADE80)
                    else -> Color(0xFF94A3B8)
                }
                Text(
                    text = stateText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = stateColor
                )
            }
        }
    }
}

// File Picker Single Row
@Composable
private fun FilePickerRow(
    label: String,
    fileName: String,
    isLoaded: Boolean,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isLoaded) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isLoaded) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular pick button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isLoaded) Color(0xFFDCFCE7) else Color(0xFFEFF6FF))
                    .clickable { onPickFile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Pick $label",
                    tint = if (isLoaded) Color(0xFF16A34A) else Color(0xFF1D4ED8),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text(
                    text = fileName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLoaded) Color(0xFF14532D) else Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLoaded) {
                IconButton(onClick = onClearFile, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// Compact Partition Table Row
@Composable
private fun CompactPartitionRow(
    partition: PartitionEntry,
    isSelected: Boolean,
    onCheckChange: () -> Unit,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = partition.isSelectedForFlashing,
                onCheckedChange = { onCheckChange() },
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF1D4ED8),
                    uncheckedColor = Color(0xFF94A3B8)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("${partition.partitionIndex}", fontSize = 10.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, modifier = Modifier.width(26.dp))
            
            Row(modifier = Modifier.width(85.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = partition.partitionName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (partition.isProtectedNv) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.Lock, contentDescription = "Protected", tint = Color(0xFFEF4444), modifier = Modifier.size(10.dp))
                }
            }

            Text(
                text = if (partition.fileName.isNotEmpty() && partition.fileName != "NONE") partition.fileName else "Auto-Linked",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (partition.fileName.isNotEmpty() && partition.fileName != "NONE") Color(0xFF16A34A) else Color(0xFF94A3B8),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = partition.linearStartAddrHex,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B),
                modifier = Modifier.width(80.dp),
                maxLines = 1
            )

            Text(
                text = partition.partitionSizeHex,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B),
                modifier = Modifier.width(60.dp),
                maxLines = 1
            )
        }
    }
}

// Reusable Dropdown Component
@Composable
private fun ToolDropdown(
    label: String,
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(Color.White)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, fontSize = 12.sp, color = Color(0xFF0F172A)) },
                        onClick = {
                            onSelect(item)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}
