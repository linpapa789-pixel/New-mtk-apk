package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Usb
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNavDestination
import com.example.model.BackupMode
import com.example.model.FlashOptions
import com.example.model.LogLevel
import com.example.model.MtkBrand
import com.example.model.MtkChipInfo
import com.example.model.MtkDeviceDatabase
import com.example.model.MtkDeviceModel
import com.example.model.OperationProgress
import com.example.model.PartitionEntry
import com.example.model.ServiceFunction
import com.example.model.TerminalLog
import com.example.model.TransportType
import com.example.protocol.TargetPhoneState
import com.example.ui.theme.TerminalTimestamp
import com.example.viewmodel.MtkBridgeViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun UnlockToolFlashScreen(
    viewModel: MtkBridgeViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val transportType by viewModel.selectedTransportType.collectAsState()
    val chipInfo by viewModel.chipInfo.collectAsState()
    val targetPhoneState by viewModel.targetPhoneState.collectAsState()
    val autoNvBackup by viewModel.autoNvBackup.collectAsState()
    val autoReboot by viewModel.autoReboot.collectAsState()
    val partitions by viewModel.partitions.collectAsState()
    val progress by viewModel.operationProgress.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val flashOptions by viewModel.flashOptions.collectAsState()
    val backupMode by viewModel.backupMode.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val scatterFileName by viewModel.scatterFileName.collectAsState()
    val scatterPlatform by viewModel.scatterPlatform.collectAsState()

    var showStopConfirmDialog by remember { mutableStateOf(false) }

    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    Text("Stop Operation?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                }
            },
            text = {
                Text(
                    "Warning: Interrupting active partition flashing or formatting may risk corrupting device storage.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B1120))
    ) {
        // 1. Top Compact Status Bar with Drawer Hamburger, Section Tag & USB Banner
        TopCompactStatusBar(
            currentDestination = currentDestination,
            transportType = transportType,
            chipInfo = chipInfo,
            targetPhoneState = targetPhoneState,
            autoNvBackup = autoNvBackup,
            onToggleNv = { viewModel.toggleAutoNvBackup(!autoNvBackup) },
            onOpenDrawer = onOpenDrawer
        )

        // 2. Main Dynamic Content Switcher (Crossfade)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Crossfade(targetState = currentDestination, label = "tab_crossfade") { dest ->
                when (dest) {
                    AppNavDestination.FLASH -> {
                        FlashScreenTab(
                            viewModel = viewModel,
                            selectedBrand = selectedBrand,
                            selectedModel = selectedModel,
                            scatterFileName = scatterFileName,
                            scatterPlatform = scatterPlatform,
                            flashOptions = flashOptions,
                            partitions = partitions,
                            progress = progress,
                            logs = logs,
                            onStopClick = { viewModel.cancelCurrentOperation() }
                        )
                    }
                    AppNavDestination.BACKUP -> {
                        BackupScreenTab(
                            viewModel = viewModel,
                            selectedBrand = selectedBrand,
                            selectedModel = selectedModel,
                            backupMode = backupMode,
                            autoReboot = autoReboot,
                            partitions = partitions,
                            progress = progress,
                            logs = logs,
                            onStopClick = { viewModel.cancelCurrentOperation() }
                        )
                    }
                    AppNavDestination.SERVICE -> {
                        ServiceScreenTab(
                            viewModel = viewModel,
                            selectedBrand = selectedBrand,
                            selectedModel = selectedModel,
                            autoReboot = autoReboot,
                            autoNvBackup = autoNvBackup,
                            progress = progress,
                            logs = logs,
                            onStopClick = { viewModel.cancelCurrentOperation() }
                        )
                    }
                    AppNavDestination.FASTBOOT -> {
                        FastbootScreenTab(
                            viewModel = viewModel,
                            progress = progress,
                            logs = logs,
                            onStopClick = { viewModel.cancelCurrentOperation() }
                        )
                    }
                    AppNavDestination.ADB -> {
                        AdbScreenTab(
                            viewModel = viewModel,
                            progress = progress,
                            logs = logs,
                            onStopClick = { viewModel.cancelCurrentOperation() }
                        )
                    }
                    AppNavDestination.OTHER -> {
                        OtherScreenTab(
                            viewModel = viewModel,
                            selectedBrand = selectedBrand,
                            selectedModel = selectedModel,
                            progress = progress,
                            logs = logs,
                            onStopClick = { viewModel.cancelCurrentOperation() }
                        )
                    }
                }
            }
        }

        // 4. Fixed Non-Intrusive Bottom Loading Indicator (Progress %, Speed, ETA)
        CompactOperationFooter(progress = progress)
    }
}

// =============================================================================
// 1️⃣ FLASH SCREEN TAB
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashScreenTab(
    viewModel: MtkBridgeViewModel,
    selectedBrand: MtkBrand,
    selectedModel: MtkDeviceModel,
    scatterFileName: String,
    scatterPlatform: String,
    flashOptions: FlashOptions,
    partitions: List<PartitionEntry>,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    val context = LocalContext.current

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
                viewModel.addLog(TerminalLog(nowTime(), "Failed to load scatter: ${e.message}", LogLevel.ERROR))
            }
        }
    }

    val daPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "MTK_AllInOne_DA.bin"
            viewModel.customDaPath.value = fileName
            viewModel.addLog(TerminalLog(nowTime(), "Custom DA Bound: $fileName", LogLevel.INFO))
        }
    }

    val preloaderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "preloader.bin"
            viewModel.preloaderPath.value = fileName
            viewModel.addLog(TerminalLog(nowTime(), "Custom Preloader Bound: $fileName", LogLevel.INFO))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Options & File Control Card
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Brand & Model Selection
                BrandModelSelectorRow(
                    selectedBrand = selectedBrand,
                    selectedModel = selectedModel,
                    onBrandSelect = { viewModel.selectBrand(it) },
                    onModelSelect = { viewModel.selectModel(it) }
                )

                // Row 2: Custom DA, Preloader, Scatter File Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactFilePickerButton(
                        label = "Scatter: ${if (scatterFileName.isEmpty()) "Select" else scatterFileName}",
                        icon = Icons.Default.FileOpen,
                        isHighlighted = scatterFileName.isNotEmpty(),
                        modifier = Modifier.weight(1.3f),
                        onClick = { scatterPickerLauncher.launch("*/*") }
                    )
                    CompactFilePickerButton(
                        label = "DA: Universal",
                        icon = Icons.Default.Security,
                        modifier = Modifier.weight(1f),
                        onClick = { daPickerLauncher.launch("*/*") }
                    )
                    CompactFilePickerButton(
                        label = "Preloader",
                        icon = Icons.Default.Memory,
                        modifier = Modifier.weight(1f),
                        onClick = { preloaderPickerLauncher.launch("*/*") }
                    )
                }

                // Row 3: 6 Auto Action Checkboxes (User Requested Strict 6 Checkboxes)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    maxItemsInEachRow = 3
                ) {
                    FlashCheckboxItem(label = "Read NV Data", checked = flashOptions.readNvData, onCheckedChange = { viewModel.toggleFlashReadNvData(it) })
                    FlashCheckboxItem(label = "Auto Reboot", checked = flashOptions.autoReboot, onCheckedChange = { viewModel.toggleFlashAutoReboot(it) })
                    FlashCheckboxItem(label = "Flash After BL Unlock", checked = flashOptions.flashAfterBlUnlock, onCheckedChange = { viewModel.toggleFlashAfterBlUnlock(it) })
                    FlashCheckboxItem(label = "DA DL Checksum", checked = flashOptions.daDlChecksum, onCheckedChange = { viewModel.toggleFlashDaDlChecksum(it) })
                    FlashCheckboxItem(label = "Auto Sign Flash", checked = flashOptions.autoSignFlash, onCheckedChange = { viewModel.toggleFlashAutoSign(it) })
                    FlashCheckboxItem(label = "Format All Download", checked = flashOptions.formatAllDownload, isWarning = true, onCheckedChange = { viewModel.toggleFlashFormatAll(it) })
                }

                // Action Controls Row: Scan, Dynamic START/STOP Flash Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { viewModel.scanTargetPhone() },
                        enabled = !progress.isRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Scan & Read GPT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                    }

                    // Dynamic START / STOP Button (Toggles text and action)
                    Button(
                        onClick = {
                            if (progress.isRunning) {
                                onStopClick()
                            } else {
                                viewModel.executeFlashOperation()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (progress.isRunning) Color(0xFFDC2626) else Color(0xFF16A34A)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1.3f).height(34.dp).testTag("start_flash_button")
                    ) {
                        Icon(
                            imageVector = if (progress.isRunning) Icons.Default.Stop else Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (progress.isRunning) "STOP FLASHING" else "START FLASHING",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Partition Table (Dynamic Scatter or GPT)
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth().weight(0.7f)
        ) {
            PartitionTableView(
                partitions = partitions,
                onToggleAll = { checked -> viewModel.toggleAllPartitions(checked) },
                onTogglePartition = { idx, checked -> viewModel.togglePartitionSelection(idx, checked) }
            )
        }

        // Live Terminal Log Box (Maximum Screen Space)
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(1.3f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() },
                onAiHelp = { viewModel.requestAiLogAnalysis() }
            )
        }
    }
}

// =============================================================================
// 2️⃣ BACKUP SCREEN TAB
// =============================================================================
@Composable
private fun BackupScreenTab(
    viewModel: MtkBridgeViewModel,
    selectedBrand: MtkBrand,
    selectedModel: MtkDeviceModel,
    backupMode: BackupMode,
    autoReboot: Boolean,
    partitions: List<PartitionEntry>,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Brand / Model
                BrandModelSelectorRow(
                    selectedBrand = selectedBrand,
                    selectedModel = selectedModel,
                    onBrandSelect = { viewModel.selectBrand(it) },
                    onModelSelect = { viewModel.selectModel(it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Backup Mode:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    FlashCheckboxItem(
                        label = "Auto Reboot after read",
                        checked = autoReboot,
                        onCheckedChange = { viewModel.toggleAutoReboot(it) }
                    )
                }

                // 4 Backup Mode Option Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BackupMode.values().forEach { mode ->
                        val isSelected = backupMode == mode
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setBackupMode(mode) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.shortLabel,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Backup Mode Description Box
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0B1120),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ℹ️ ${backupMode.description}",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Action Controls Row: Dynamic START / STOP Backup Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            if (progress.isRunning) {
                                onStopClick()
                            } else {
                                viewModel.executeBackupOperation()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (progress.isRunning) Color(0xFFDC2626) else Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp).testTag("start_backup_button")
                    ) {
                        Icon(
                            imageVector = if (progress.isRunning) Icons.Default.Stop else Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (progress.isRunning) "STOP BACKUP PROCESS" else "START BACKUP / READ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Live GPT Table Preview
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth().weight(0.7f)
        ) {
            PartitionTableView(
                partitions = partitions,
                onToggleAll = { checked -> viewModel.toggleAllPartitions(checked) },
                onTogglePartition = { idx, checked -> viewModel.togglePartitionSelection(idx, checked) }
            )
        }

        // Live Terminal Log Box (Maximum Screen Space)
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(1.3f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() },
                onAiHelp = { viewModel.requestAiLogAnalysis() }
            )
        }
    }
}

// =============================================================================
// 3️⃣ SERVICE / UNLOCK SCREEN TAB
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceScreenTab(
    viewModel: MtkBridgeViewModel,
    selectedBrand: MtkBrand,
    selectedModel: MtkDeviceModel,
    autoReboot: Boolean,
    autoNvBackup: Boolean,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Brand / Model Selector
                BrandModelSelectorRow(
                    selectedBrand = selectedBrand,
                    selectedModel = selectedModel,
                    onBrandSelect = { viewModel.selectBrand(it) },
                    onModelSelect = { viewModel.selectModel(it) }
                )

                // Safety Options Checkboxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlashCheckboxItem(
                        label = "Auto Reboot",
                        checked = autoReboot,
                        onCheckedChange = { viewModel.toggleAutoReboot(it) }
                    )
                    FlashCheckboxItem(
                        label = "Auto NV Backup (Safety Guard)",
                        checked = autoNvBackup,
                        onCheckedChange = { viewModel.toggleAutoNvBackup(it) }
                    )
                }

                Text("One-Click GSM Service Functions:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))

                if (progress.isRunning) {
                    Button(
                        onClick = onStopClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("STOP ACTIVE SERVICE (${progress.title.ifEmpty { "RUNNING" }})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // One-Click Actions Grid (Real protocol execution)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3
                ) {
                    ServiceActionButton(
                        label = "Erase FRP",
                        icon = Icons.Default.LockOpen,
                        accentColor = Color(0xFFF59E0B),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.ERASE_FRP) }
                    )
                    ServiceActionButton(
                        label = "Factory Reset",
                        icon = Icons.Default.LockReset,
                        accentColor = Color(0xFFEF4444),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.FACTORY_RESET) }
                    )
                    ServiceActionButton(
                        label = "Unlock BL",
                        icon = Icons.Default.LockOpen,
                        accentColor = Color(0xFF10B981),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.UNLOCK_BOOTLOADER) }
                    )
                    ServiceActionButton(
                        label = "Relock BL",
                        icon = Icons.Default.Security,
                        accentColor = Color(0xFF64748B),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.LOCK_BOOTLOADER) }
                    )
                    ServiceActionButton(
                        label = "Disable Mi Acc",
                        icon = Icons.Default.Security,
                        accentColor = Color(0xFFF97316),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.DISABLE_MI_ACCOUNT) }
                    )
                    ServiceActionButton(
                        label = "Restore NVRAM",
                        icon = Icons.Default.Shield,
                        accentColor = Color(0xFF8B5CF6),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.RESTORE_NVRAM) }
                    )
                    ServiceActionButton(
                        label = "Read Info",
                        icon = Icons.Default.Memory,
                        accentColor = Color(0xFF06B6D4),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.READ_INFO) }
                    )
                    ServiceActionButton(
                        label = "Read RPMB",
                        icon = Icons.Default.Security,
                        accentColor = Color(0xFF6366F1),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.READ_RPMB) }
                    )
                    ServiceActionButton(
                        label = "Crash to BROM",
                        icon = Icons.Default.Build,
                        accentColor = Color(0xFFEC4899),
                        enabled = !progress.isRunning,
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.CRASH_TO_BROM) }
                    )
                }

                // Reboot Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RebootPillButton(label = "Reboot System", modifier = Modifier.weight(1f)) {
                        viewModel.executeServiceFunctionDirect(ServiceFunction.REBOOT_SYSTEM)
                    }
                    RebootPillButton(label = "Reboot Fastboot", modifier = Modifier.weight(1f)) {
                        viewModel.executeServiceFunctionDirect(ServiceFunction.REBOOT_FASTBOOT)
                    }
                    RebootPillButton(label = "Reboot Recovery", modifier = Modifier.weight(1f)) {
                        viewModel.executeServiceFunctionDirect(ServiceFunction.REBOOT_RECOVERY)
                    }
                }
            }
        }

        // Live Terminal Log Box (Spacious for service operations)
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() },
                onAiHelp = { viewModel.requestAiLogAnalysis() }
            )
        }
    }
}

// =============================================================================
// 4️⃣ OTHER & ADVANCED SCREEN TAB
// =============================================================================
@Composable
private fun OtherScreenTab(
    viewModel: MtkBridgeViewModel,
    selectedBrand: MtkBrand,
    selectedModel: MtkDeviceModel,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Brand / Model Selector
                BrandModelSelectorRow(
                    selectedBrand = selectedBrand,
                    selectedModel = selectedModel,
                    onBrandSelect = { viewModel.selectBrand(it) },
                    onModelSelect = { viewModel.selectModel(it) }
                )

                Text("Advanced Hardware & Security Tools:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))

                if (progress.isRunning) {
                    Button(
                        onClick = onStopClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("STOP ACTIVE OPERATION (${progress.title.ifEmpty { "RUNNING" }})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AdvancedActionCard(
                        title = "Memory Test",
                        subtitle = "RAM & eMMC/UFS Diagnostic",
                        icon = Icons.Default.Memory,
                        accentColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.runMemoryTest() }
                    )
                    AdvancedActionCard(
                        title = "SLA/DAA Bypass",
                        subtitle = "Kamakiri Auth Exploit",
                        icon = Icons.Default.Security,
                        accentColor = Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.BYPASS_AUTH) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AdvancedActionCard(
                        title = "Watchdog Reset",
                        subtitle = "USB Control Transfer Reset",
                        icon = Icons.Default.Refresh,
                        accentColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.executeServiceFunctionDirect(ServiceFunction.CRASH_TO_BROM) }
                    )
                    AdvancedActionCard(
                        title = "Gemini AI Advisor",
                        subtitle = "Deep Log & Error Diagnosis",
                        icon = Icons.Default.AutoAwesome,
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.requestAiLogAnalysis() }
                    )
                }
            }
        }

        // Live Terminal Log Box
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() },
                onAiHelp = { viewModel.requestAiLogAnalysis() }
            )
        }
    }
}

// =============================================================================
// REUSABLE SUB-COMPONENTS
// =============================================================================

@Composable
private fun BrandModelSelectorRow(
    selectedBrand: MtkBrand,
    selectedModel: MtkDeviceModel,
    onBrandSelect: (MtkBrand) -> Unit,
    onModelSelect: (MtkDeviceModel) -> Unit
) {
    var expBrand by remember { mutableStateOf(false) }
    var expModel by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Brand Dropdown
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expBrand = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedBrand.brandName,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text("▼", fontSize = 8.sp, color = Color(0xFF94A3B8))
                }
            }

            DropdownMenu(
                expanded = expBrand,
                onDismissRequest = { expBrand = false }
            ) {
                MtkDeviceDatabase.brands.forEach { brand ->
                    DropdownMenuItem(
                        text = { Text(brand.brandName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onBrandSelect(brand)
                            expBrand = false
                        }
                    )
                }
            }
        }

        // Model Dropdown
        Box(modifier = Modifier.weight(1.3f)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expModel = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedModel.modelName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("▼", fontSize = 8.sp, color = Color(0xFF94A3B8))
                }
            }

            DropdownMenu(
                expanded = expModel,
                onDismissRequest = { expModel = false }
            ) {
                selectedBrand.models.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.modelName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(model.chipset, fontSize = 9.sp, color = Color(0xFF64748B))
                            }
                        },
                        onClick = {
                            onModelSelect(model)
                            expModel = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashCheckboxItem(
    label: String,
    checked: Boolean,
    isWarning: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 1.dp, horizontal = 2.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = label,
            tint = if (checked) (if (isWarning) Color(0xFFEF4444) else Color(0xFF38BDF8)) else Color(0xFF64748B),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            color = if (checked) (if (isWarning) Color(0xFFFCA5A5) else Color(0xFFF1F5F9)) else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun CompactFilePickerButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isHighlighted) Color(0xFF0F3E6D) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isHighlighted) Color(0xFF38BDF8) else Color(0xFF334155)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (isHighlighted) Color.White else Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ServiceActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .width(108.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RebootPillButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF475569)),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            modifier = Modifier.padding(vertical = 5.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AdvancedActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 8.5.sp, color = Color(0xFF94A3B8), maxLines = 1)
            }
        }
    }
}

// =============================================================================
// DYNAMIC PARTITION TABLE VIEW
// =============================================================================
@Composable
private fun PartitionTableView(
    partitions: List<PartitionEntry>,
    onToggleAll: (Boolean) -> Unit,
    onTogglePartition: (Int, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val allChecked = partitions.isNotEmpty() && partitions.all { it.isSelectedForFlashing }
            IconButton(
                onClick = { onToggleAll(!allChecked) },
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = if (allChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Toggle All",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text("Partition", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.2f))
            Text("Offset", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(0.9f))
            Text("Size", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(0.7f))
            Text("Region", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(0.8f))
        }

        HorizontalDivider(color = Color(0xFF334155))

        if (partitions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No partitions loaded.\nConnect MTK phone to read GPT or load Scatter file.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(partitions) { idx, part ->
                    val isRowSelected = part.isSelectedForFlashing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isRowSelected) Color(0xFF1E293B) else Color.Transparent)
                            .clickable { onTogglePartition(idx, !isRowSelected) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isRowSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = part.partitionName,
                            tint = if (isRowSelected) Color(0xFF38BDF8) else Color(0xFF475569),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = part.partitionName,
                            fontSize = 10.sp,
                            fontWeight = if (isRowSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRowSelected) Color.White else Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1.2f),
                            maxLines = 1
                        )
                        Text(
                            text = part.linearStartAddrHex,
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(0.9f),
                            maxLines = 1
                        )
                        Text(
                            text = part.formattedSize,
                            fontSize = 9.sp,
                            color = Color(0xFF4ADE80),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(0.7f),
                            maxLines = 1
                        )
                        Text(
                            text = part.region,
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.weight(0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPACT TERMINAL LOG VIEW
// =============================================================================
@Composable
private fun CompactTerminalLogView(
    logs: List<TerminalLog>,
    onClear: () -> Unit,
    onAiHelp: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val logListState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = Color(0xFF10B981), modifier = Modifier.size(6.dp)) {}
                Text("Live Terminal Output", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF4C1D95),
                    modifier = Modifier.clickable { onAiHelp() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Help", tint = Color(0xFFE9D5FF), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("AI Help", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9D5FF))
                    }
                }

                IconButton(
                    onClick = {
                        val text = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                        clipboardManager.setText(AnnotatedString(text))
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 2.dp))

        LazyColumn(
            state = logListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(logs) { log ->
                val color = when (log.level) {
                    LogLevel.SUCCESS -> Color(0xFF10B981)
                    LogLevel.ERROR -> Color(0xFFF87171)
                    LogLevel.WARNING -> Color(0xFFFBBF24)
                    LogLevel.AI -> Color(0xFFA78BFA)
                    LogLevel.ACCENT -> Color(0xFF38BDF8)
                    LogLevel.CYAN -> Color(0xFF22D3EE)
                    LogLevel.MAGENTA -> Color(0xFFF472B6)
                    LogLevel.RAW -> Color(0xFF94A3B8)
                    LogLevel.INFO -> Color(0xFFF1F5F9)
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text("[${log.timestamp}] ", fontSize = 10.sp, color = TerminalTimestamp, fontFamily = FontFamily.Monospace)
                    Text(log.message, fontSize = 10.sp, color = color, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
                }
            }
        }
    }
}

// =============================================================================
// 4️⃣ FASTBOOT SCREEN TAB
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FastbootScreenTab(
    viewModel: MtkBridgeViewModel,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    val isFastbootBusy by viewModel.isFastbootBusy.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()
    val fastbootInfo by viewModel.fastbootDeviceInfo.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Upper Controls & Action Cards
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header & Info Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Text("Fastboot Protocol Engine", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                    Text(if (isDryRun) "SIMULATION ACTIVE" else "LIVE FASTBOOT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Section 1: Device Variables & ID
                Text("IDENTIFICATION & VARIABLES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.runFastbootReadAllVars() },
                        enabled = !isFastbootBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("📋 GetVar:All (Info)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.runFastbootCommand("Check Unlocked", "getvar:unlocked") },
                        enabled = !isFastbootBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🔑 Check Lock Status", fontSize = 11.sp, color = Color.White)
                    }
                }

                // Section 2: Bootloader & Partition Actions
                Text("BOOTLOADER & PARTITION UNLOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.runFastbootUnlockBootloader() },
                        enabled = !isFastbootBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🔓 Flashing Unlock", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                    Button(
                        onClick = { viewModel.runFastbootLockBootloader() },
                        enabled = !isFastbootBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🔒 Flashing Lock", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.runFastbootEraseFrp() },
                        enabled = !isFastbootBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🗑️ Erase FRP (Fastboot)", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                    Button(
                        onClick = { viewModel.runFastbootFormatUserdata() },
                        enabled = !isFastbootBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE123C)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("⚡ Format Userdata (Wipe)", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }

                // Section 3: Fastboot Reboot Switcher
                Text("REBOOT MODE SWITCHER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.runFastbootReboot("system") },
                        enabled = !isFastbootBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot System", fontSize = 10.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.runFastbootReboot("recovery") },
                        enabled = !isFastbootBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot Recovery", fontSize = 10.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.runFastbootReboot("fastbootd") },
                        enabled = !isFastbootBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot FastbootD", fontSize = 10.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.runFastbootReboot("edl") },
                        enabled = !isFastbootBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot EDL / BROM", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }

        // Lower: Real-time Terminal Log (Spacious view)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() },
                onAiHelp = { viewModel.requestAiLogAnalysis() }
            )
        }
    }
}

// =============================================================================
// 5️⃣ ADB SCREEN TAB
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdbScreenTab(
    viewModel: MtkBridgeViewModel,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    val isAdbBusy by viewModel.isAdbBusy.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Upper Controls & Action Cards
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header & Info Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(16.dp))
                        Text("Android Debug Bridge (ADB)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                    }
                    Text(if (isDryRun) "SIMULATION ACTIVE" else "LIVE ADB HOST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Section 1: Device Information
                Text("DEVICE INFORMATION & STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.runAdbReadInfo() },
                        enabled = !isAdbBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("📱 Read Full Device Info", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.runAdbCommand("Battery Info", "dumpsys battery | grep -E 'level|status|health|temperature'") },
                        enabled = !isAdbBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🔋 Battery & Health", fontSize = 11.sp, color = Color.White)
                    }
                }

                // Section 2: FRP & Service Bypass
                Text("BYPASS & SERVICE ENABLER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.runAdbBypassFrp() },
                        enabled = !isAdbBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🔓 Bypass Setup Wizard (FRP)", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                    Button(
                        onClick = { viewModel.runAdbEnableLanguages() },
                        enabled = !isAdbBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🌐 Enable All Languages (MoreLocale)", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }

                // Section 3: Bloatware Remover
                Text("BLOATWARE REMOVER (SYSTEM CLEANUP)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val miBloat = listOf(
                                "com.miui.analytics", "com.miui.msa.global", "com.xiaomi.glgm",
                                "com.miui.bugreport", "com.miui.cleanmaster"
                            )
                            viewModel.runAdbRemoveBloatware(miBloat)
                        },
                        enabled = !isAdbBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🗑️ Disable Xiaomi Bloatware", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            val oppoBloat = listOf(
                                "com.heytap.mcs", "com.heytap.themestore", "com.oppo.market",
                                "com.nearme.gamecenter"
                            )
                            viewModel.runAdbRemoveBloatware(oppoBloat)
                        },
                        enabled = !isAdbBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("🗑️ Disable Oppo/Realme Bloat", fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }

                // Section 4: ADB Reboot Options
                Text("REBOOT DESTINATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.runAdbReboot("system") },
                        enabled = !isAdbBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot System", fontSize = 10.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.runAdbReboot("bootloader") },
                        enabled = !isAdbBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot to Fastboot", fontSize = 10.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.runAdbReboot("recovery") },
                        enabled = !isAdbBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot to Recovery", fontSize = 10.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.runAdbReboot("edl") },
                        enabled = !isAdbBusy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reboot to BROM / EDL", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }

        // Lower: Real-time Terminal Log (Spacious view)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() },
                onAiHelp = { viewModel.requestAiLogAnalysis() }
            )
        }
    }
}
@Composable
private fun CompactOperationFooter(progress: OperationProgress) {
    Surface(
        color = Color(0xFF020617),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (progress.isRunning) progress.title else "System Ready (Direct USB OTG Host)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress.isRunning) Color(0xFF38BDF8) else Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val speedText = if (progress.speedKbPerSec > 1024) {
                    "%.2f MB/s".format(progress.speedKbPerSec / 1024.0)
                } else if (progress.speedKbPerSec > 0) {
                    "%.1f KB/s".format(progress.speedKbPerSec)
                } else {
                    "-- MB/s"
                }

                val etaText = if (progress.estimatedSecondsRemaining > 0) {
                    val m = progress.estimatedSecondsRemaining / 60
                    val s = progress.estimatedSecondsRemaining % 60
                    "ETA: %02d:%02d".format(m, s)
                } else {
                    "ETA: --"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (progress.isRunning) {
                        Text(
                            text = "$speedText | $etaText",
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", progress.percentage)}%",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.isRunning) Color(0xFF4ADE80) else Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (progress.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = if (progress.isRunning) Color(0xFF38BDF8) else Color(0xFF334155),
                trackColor = Color(0xFF0F172A)
            )
        }
    }
}

// =============================================================================
// COMPACT TOP STATUS BAR
// =============================================================================
@Composable
private fun TopCompactStatusBar(
    currentDestination: AppNavDestination,
    transportType: TransportType,
    chipInfo: MtkChipInfo,
    targetPhoneState: TargetPhoneState,
    autoNvBackup: Boolean,
    onToggleNv: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isConnected = targetPhoneState is TargetPhoneState.Connected
    val isSim = transportType == TransportType.SIMULATION

    val (bannerBg, bannerBorder, bannerIconTint, bannerTitle) = when {
        isConnected -> {
            val isBrom = (targetPhoneState as TargetPhoneState.Connected).isBromMode
            val modeName = if (isBrom) "BROM Mode" else "Preloader Mode"
            arrayOf(
                Color(0xFF064E3B),
                Color(0xFF059669),
                Color(0xFF34D399),
                "CONNECTED: $modeName (${targetPhoneState.vidPid})"
            )
        }
        isSim -> arrayOf(
            Color(0xFF1E293B),
            Color(0xFF3B82F6),
            Color(0xFF60A5FA),
            "SIMULATION (Virtual MT6765)"
        )
        else -> arrayOf(
            Color(0xFF1E1B4B),
            Color(0xFF4338CA),
            Color(0xFF818CF8),
            "USB OTG HOST READY"
        )
    }

    Surface(
        color = Color(0xFF020617),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onOpenDrawer() }
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu Drawer", tint = Color.White, modifier = Modifier.size(17.dp))
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1D4ED8),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(currentDestination.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text(currentDestination.shortTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(3.dp),
                color = bannerBg as Color,
                border = BorderStroke(1.dp, bannerBorder as Color)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Usb, contentDescription = null, tint = bannerIconTint as Color, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = bannerTitle as String,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun nowTime(): String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
