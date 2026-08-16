package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.model.MtkBrand
import com.example.model.MtkDeviceDatabase
import com.example.model.MtkDeviceModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
import com.example.model.ServiceFunction
import com.example.model.TerminalLog
import com.example.model.TransportType
import com.example.protocol.TargetPhoneState
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
    val chipInfo by viewModel.chipInfo.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val transportType by viewModel.selectedTransportType.collectAsState()
    val targetPhoneState by viewModel.targetPhoneState.collectAsState()
    val scatterPath by viewModel.scatterPath.collectAsState()
    val daPath by viewModel.daAgentPath.collectAsState()
    val preloaderPath by viewModel.preloaderPath.collectAsState()
    val autoNvBackup by viewModel.autoNvBackup.collectAsState()

    // Form Dropdown states
    var selectedConnMode by remember { mutableStateOf("Direct USB OTG (Host)") }

    // Dropdown expansion states
    var expBrand by remember { mutableStateOf(false) }
    var expModel by remember { mutableStateOf(false) }
    var expConnMode by remember { mutableStateOf(false) }
    var expServiceFunc by remember { mutableStateOf(false) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    // Stop Confirmation Dialog
    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    Text("Stop Flashing Process?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                }
            },
            text = {
                Text(
                    "Warning: Are you sure you want to stop the active operation?\n\nInterrupting active partition writes (such as preloader, lk, or boot) might risk bricking the target device.",
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            },
            containerColor = Color(0xFF1E293B),
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmDialog = false
                        viewModel.cancelCurrentOperation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Yes, Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showStopConfirmDialog = false },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("Cancel", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Log terminal list state for auto-scroll
    val logListState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    // System File Pickers
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
            viewModel.addLog(TerminalLog(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), "Selected DA: $fileName", LogLevel.INFO))
        }
    }

    val preloaderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "preloader.bin"
            viewModel.preloaderPath.value = fileName
            viewModel.addLog(TerminalLog(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), "Selected Preloader: $fileName", LogLevel.INFO))
        }
    }

    // MAIN FULL-SCREEN CONTAINER (Balanced Fit Layout)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Status Bar (Ultra Compact)
        TopCompactStatusBar(
            transportType = transportType,
            chipInfo = chipInfo,
            targetPhoneState = targetPhoneState,
            autoNvBackup = autoNvBackup,
            onToggleNv = { viewModel.toggleAutoNvBackup(!autoNvBackup) },
            onOpenDrawer = onOpenDrawer
        )

        // Workspace Column filling remaining space
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // =================================================================
            // 1️⃣ TOP SECTION (Ultra-Compact Sleek Settings & Actions Card)
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    // Row 1: Brand, Model & Mode (No unnecessary SoC dropdown)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactToolDropdown(
                            label = "Brand",
                            selectedText = selectedBrand.brandName.substringBefore(" (").substringBefore(" /"),
                            expanded = expBrand,
                            onExpandedChange = { expBrand = it },
                            items = MtkDeviceDatabase.brands.map { it.brandName },
                            onSelect = { brandName ->
                                MtkDeviceDatabase.brands.find { it.brandName == brandName }?.let {
                                    viewModel.selectBrand(it)
                                }
                            },
                            modifier = Modifier.weight(1.15f)
                        )

                        CompactToolDropdown(
                            label = "Model",
                            selectedText = selectedModel.modelName,
                            expanded = expModel,
                            onExpandedChange = { expModel = it },
                            items = selectedBrand.models.map { "${it.modelName} [${it.chipset}]" },
                            onSelect = { fullStr ->
                                selectedBrand.models.find { "${it.modelName} [${it.chipset}]" == fullStr }?.let {
                                    viewModel.selectModel(it)
                                }
                            },
                            modifier = Modifier.weight(1.85f)
                        )

                        CompactToolDropdown(
                            label = "Mode",
                            selectedText = if (selectedConnMode.contains("Simulation")) "Sim" else "OTG",
                            expanded = expConnMode,
                            onExpandedChange = { expConnMode = it },
                            items = listOf("OTG (Direct USB)", "Sim (Simulation)"),
                            onSelect = {
                                selectedConnMode = it
                                if (it.contains("Sim")) {
                                    viewModel.setTransportType(TransportType.SIMULATION)
                                } else {
                                    viewModel.setTransportType(TransportType.USB_OTG_DIRECT)
                                }
                            },
                            modifier = Modifier.weight(0.85f)
                        )
                    }

                    // Row 2: Firmware Files (Scatter, Custom DA, Custom Preloader) - Equal 1:1:1 Symmetrical Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scatter / Partition File Button
                        CompactMiniFileBtn(
                            label = "Scatter",
                            fileName = scatterPath.ifEmpty { "Default" },
                            isLoaded = scatterPath.isNotEmpty() || partitions.isNotEmpty(),
                            onPick = { scatterPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        )

                        // Custom DA Agent File Button
                        CompactMiniFileBtn(
                            label = "Custom DA",
                            fileName = daPath.ifEmpty { "Built-in DA" },
                            isLoaded = daPath.isNotEmpty() && !daPath.startsWith("Built-in"),
                            onPick = { daPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        )

                        // Custom Preloader File Button
                        CompactMiniFileBtn(
                            label = "Custom PL",
                            fileName = preloaderPath.ifEmpty { "Auto PL" },
                            isLoaded = preloaderPath.isNotEmpty(),
                            onPick = { preloaderPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: Service Task Function & Controls (RST, START / STOP)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Service Task Function Selector (Abbreviated Titles)
                        Box(modifier = Modifier.weight(1.4f)) {
                            val shortTaskTitle = when (selectedServiceFunction) {
                                ServiceFunction.READ_INFO -> "Read Info"
                                ServiceFunction.WRITE_PARTITION -> "Write Part"
                                ServiceFunction.BATCH_FLASH -> "Batch Flash"
                                ServiceFunction.READ_PARTITION -> "Read Part"
                                ServiceFunction.DUMP_ALL_PARTITIONS -> "Dump ROM"
                                ServiceFunction.READ_PRELOADER -> "Read PL"
                                ServiceFunction.READ_GPT_SCATTER -> "Read GPT"
                                ServiceFunction.READ_RPMB -> "Read RPMB"
                                ServiceFunction.BACKUP_NVRAM -> "Backup NV"
                                ServiceFunction.RESTORE_NVRAM -> "Restore NV"
                                ServiceFunction.BYPASS_AUTH -> "Bypass Auth"
                                ServiceFunction.UNLOCK_BOOTLOADER -> "Unlock BL"
                                ServiceFunction.LOCK_BOOTLOADER -> "Lock BL"
                                ServiceFunction.ERASE_FRP -> "Erase FRP"
                                ServiceFunction.FACTORY_RESET -> "Factory Reset"
                                ServiceFunction.FORMAT_PARTITION -> "Format"
                                ServiceFunction.CRASH_TO_BROM -> "Crash BROM"
                                ServiceFunction.REBOOT_SYSTEM -> "Reboot Sys"
                                ServiceFunction.REBOOT_FASTBOOT -> "Fastboot"
                                ServiceFunction.REBOOT_RECOVERY -> "Recovery"
                            }
                            CompactToolDropdown(
                                label = "Task",
                                selectedText = shortTaskTitle,
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

                        // Watchdog Reset Button
                        OutlinedButton(
                            onClick = { viewModel.sendWatchdogReset() },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(26.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                            border = BorderStroke(1.dp, Color(0xFF78350F))
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("RST", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // START / STOP Toggle Button (Blue = START, Red = STOP)
                        Button(
                            onClick = {
                                if (progress.isRunning) {
                                    showStopConfirmDialog = true
                                } else {
                                    viewModel.executeActiveServiceFunction()
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (progress.isRunning) Color(0xFFDC2626) else Color(0xFF2563EB)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp).weight(0.95f)
                        ) {
                            Icon(
                                imageVector = if (progress.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (progress.isRunning) "STOP" else "START",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 2️⃣ MIDDLE SECTION : Partitions Table Box Card
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Partition Table Header Bar
                    val allSelected = partitions.isNotEmpty() && partitions.all { it.isSelectedForFlashing }
                    Surface(
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { viewModel.toggleSelectAllPartitions(it) },
                                modifier = Modifier.size(18.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF38BDF8),
                                    uncheckedColor = Color(0xFF64748B)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PARTITIONS TABLE (${partitions.count { it.isSelectedForFlashing }}/${partitions.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("OFFSET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, modifier = Modifier.width(68.dp))
                            Text("SIZE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, modifier = Modifier.width(52.dp))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)

                    // Partition Table Scrollable Content Box
                    if (partitions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "No Partitions Loaded",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    "1. Connect phone & tap 'START' to read Live GPT\n2. Or tap 'Scatter' to load partition layout from file",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        ) {
                            itemsIndexed(partitions) { index, part ->
                                val isSelected = selectedPartIndex == index
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectPartition(index) },
                                    color = if (isSelected) Color(0xFF334155) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = part.isSelectedForFlashing,
                                            onCheckedChange = { viewModel.togglePartitionSelection(index) },
                                            modifier = Modifier.size(16.dp),
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF38BDF8),
                                                uncheckedColor = Color(0xFF475569)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${part.partitionIndex}", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, modifier = Modifier.width(18.dp))
                                        
                                        Row(modifier = Modifier.width(80.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = part.partitionName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (part.isSelectedForFlashing) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (part.isProtectedNv) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(Icons.Default.Lock, contentDescription = "Protected", tint = Color(0xFFEF4444), modifier = Modifier.size(9.dp))
                                            }
                                        }

                                        Text(
                                            text = if (part.fileName.isNotEmpty() && part.fileName != "NONE") part.fileName else "Auto-Linked",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (part.fileName.isNotEmpty() && part.fileName != "NONE") Color(0xFF4ADE80) else Color(0xFF64748B),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = part.linearStartAddrHex,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.width(68.dp),
                                            maxLines = 1
                                        )

                                        Text(
                                            text = part.partitionSizeHex,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.width(52.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.4f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3️⃣ BOTTOM SECTION : Live Log Terminal Box Card
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    // Terminal Top Bar (Title, AI Diagnostic, Copy, Clear)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE LOG CONSOLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Gemini AI Quick Diagnose Button
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF581C87),
                                modifier = Modifier.clickable { viewModel.requestAiDiagnosis() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Diagnose", tint = Color(0xFFE9D5FF), modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("AI Help", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9D5FF))
                                }
                            }

                            // Copy Log
                            IconButton(
                                onClick = {
                                    val text = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                                    clipboardManager.setText(AnnotatedString(text))
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }

                            // Clear Log
                            IconButton(
                                onClick = { viewModel.clearLogs() },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }
                        }
                    }

                    // Progress Bar (when flashing is active)
                    if (progress.isRunning) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(progress.title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                Text("${String.format("%.1f", progress.percentage)}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80), fontFamily = FontFamily.Monospace)
                            }
                            LinearProgressIndicator(
                                progress = { progress.percentage / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 3.dp))

                    // Live Log Lines (Auto-scrolled)
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
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
                                Text("[${log.timestamp}] ", fontSize = 9.5.sp, color = TerminalTimestamp, fontFamily = FontFamily.Monospace)
                                Text(log.message, fontSize = 9.5.sp, color = color, fontFamily = FontFamily.Monospace, lineHeight = 12.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Compact Top Bar with Drawer, Transport, Chip ID, and IMEI Guard Status
@Composable
private fun TopCompactStatusBar(
    transportType: TransportType,
    chipInfo: com.example.model.MtkChipInfo,
    targetPhoneState: TargetPhoneState,
    autoNvBackup: Boolean,
    onToggleNv: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Surface(
        color = Color(0xFF020617),
        border = BorderStroke(0.dp, Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = transportType.displayName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // NV Guard Pill
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (autoNvBackup) Color(0xFF1E3A8A) else Color(0xFF334155),
                    modifier = Modifier.clickable { onToggleNv() }
                ) {
                    Text(
                        text = if (autoNvBackup) "IMEI GUARD ON" else "NV GUARD OFF",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoNvBackup) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "ID: ${chipInfo.chipIdHex}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8)
                )

                val stateText = when (targetPhoneState) {
                    is TargetPhoneState.Connected -> "BROM READY"
                    else -> "IDLE"
                }
                val stateColor = when (targetPhoneState) {
                    is TargetPhoneState.Connected -> Color(0xFF4ADE80)
                    else -> Color(0xFF64748B)
                }
                Text(
                    text = stateText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = stateColor
                )
            }
        }
    }
}

// Ultra Compact Mini File Badge Button (Clean, balanced 1:1:1 grid item)
@Composable
private fun CompactMiniFileBtn(
    label: String,
    fileName: String = "",
    isLoaded: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isLoaded) Color(0xFF064E3B) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isLoaded) Color(0xFF059669) else Color(0xFF334155)),
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onPick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = if (isLoaded) Color(0xFF34D399) else Color(0xFF60A5FA),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLoaded) Color(0xFFA7F3D0) else Color(0xFFE2E8F0),
                maxLines = 1
            )
        }
    }
}

// Compact File Badge
@Composable
private fun CompactFileBadge(
    label: String,
    fileName: String,
    isLoaded: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = if (isLoaded) Color(0xFF064E3B) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isLoaded) Color(0xFF059669) else Color(0xFF334155)),
        modifier = modifier.clickable { onPick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = if (isLoaded) Color(0xFF34D399) else Color(0xFF60A5FA),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Column {
                Text(label, fontSize = 7.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                Text(
                    text = fileName,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLoaded) Color(0xFFA7F3D0) else Color(0xFFE2E8F0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Compact Tool Dropdown (With Clear Text Visibility & Tight Spacing)
@Composable
private fun CompactToolDropdown(
    label: String,
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
        Text(label, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .background(Color(0xFF1E293B))
                    .heightIn(max = 280.dp)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, fontSize = 11.sp, color = Color(0xFFE2E8F0)) },
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
