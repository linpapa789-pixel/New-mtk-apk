package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.protocol.TargetPhoneState
import com.example.ui.theme.TerminalTimestamp
import com.example.viewmodel.MtkBridgeViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data structures for Grouped Action Selection
data class ActionItem(
    val id: String,
    val title: String,
    val description: String = "",
    val accentColor: Color = Color(0xFF38BDF8)
)

data class ActionGroup(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val items: List<ActionItem>
)

@Composable
fun UnlockToolFlashScreen(
    viewModel: MtkBridgeViewModel,
    onOpenDrawer: () -> Unit
) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val targetPhoneState by viewModel.targetPhoneState.collectAsState()
    val chipInfo by viewModel.chipInfo.collectAsState()
    val progress by viewModel.operationProgress.collectAsState()
    val logs by viewModel.logs.collectAsState()

    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val scatterFileName by viewModel.scatterFileName.collectAsState()
    val scatterPlatform by viewModel.scatterPlatform.collectAsState()
    val flashOptions by viewModel.flashOptions.collectAsState()
    val partitions by viewModel.partitions.collectAsState()
    val backupMode by viewModel.backupMode.collectAsState()
    val autoReboot by viewModel.autoReboot.collectAsState()
    val autoNvBackup by viewModel.autoNvBackup.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // 1. Top Header Bar
        TopHeaderToolBar(
            chipInfo = chipInfo,
            targetPhoneState = targetPhoneState,
            onOpenDrawer = onOpenDrawer
        )

        // 2. Quick Navigation Tabs Bar
        TopNavigationTabBar(
            currentDestination = currentDestination,
            onSelectDestination = { viewModel.navigateTo(it) }
        )

        // 3. Tab Body (Crossfade Navigation)
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

        // 4. Bottom Status & Progress Footer
        CompactOperationFooter(progress = progress)
    }
}

// =============================================================================
// TOP TOOLBAR & NAVIGATION
// =============================================================================
@Composable
private fun TopHeaderToolBar(
    chipInfo: MtkChipInfo,
    targetPhoneState: TargetPhoneState,
    onOpenDrawer: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Drawer Menu",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MTK FLASHER & SERVICE TOOL",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = if (chipInfo.chipIdHex.isNotEmpty()) "Chip: ${chipInfo.chipIdHex} (${chipInfo.hwCodeHex})" else "Direct USB-OTG Host Engine",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Connection Badge
            val isConn = targetPhoneState is TargetPhoneState.Connected
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isConn) Color(0xFF064E3B) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isConn) Color(0xFF10B981) else Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isConn) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Text(
                        text = if (isConn) "USB CONNECTED" else "WAITING USB",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConn) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun TopNavigationTabBar(
    currentDestination: AppNavDestination,
    onSelectDestination: (AppNavDestination) -> Unit
) {
    val scrollState = rememberScrollState()
    Surface(
        color = Color(0xFF0B1120),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            AppNavDestination.values().forEach { dest ->
                val isSelected = currentDestination == dest
                val accentColor = when (dest) {
                    AppNavDestination.FLASH -> Color(0xFF38BDF8)
                    AppNavDestination.BACKUP -> Color(0xFF0284C7)
                    AppNavDestination.SERVICE -> Color(0xFFF59E0B)
                    AppNavDestination.FASTBOOT -> Color(0xFF10B981)
                    AppNavDestination.ADB -> Color(0xFF06B6D4)
                    AppNavDestination.OTHER -> Color(0xFFA855F7)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (isSelected) accentColor else Color.Transparent),
                    modifier = Modifier.clickable { onSelectDestination(dest) }
                ) {
                    Text(
                        text = dest.tabTitle,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE GROUPED ACTION SELECTOR + SINGLE START BUTTON
// =============================================================================
@Composable
private fun ActionGroupSelectorBox(
    groups: List<ActionGroup>,
    selectedId: String,
    onSelectId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Group Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = group.icon,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = group.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Items inside Group
                    group.items.forEach { item ->
                        val isSelected = selectedId == item.id
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0B1120).copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) item.accentColor.copy(alpha = 0.7f) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectId(item.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = if (isSelected) "Checked" else "Unchecked",
                                    tint = if (isSelected) item.accentColor else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            fontSize = 8.5.sp,
                                            color = if (isSelected) item.accentColor.copy(alpha = 0.85f) else Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleStartActionButton(
    actionTitle: String,
    isRunning: Boolean,
    enabled: Boolean = true,
    accentColor: Color = Color(0xFF059669),
    onExecute: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { if (isRunning) onStop() else onExecute() },
        enabled = if (isRunning) true else enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) Color(0xFFDC2626) else accentColor,
            disabledContainerColor = Color(0xFF334155)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isRunning) "STOP OPERATION" else "START EXECUTE : $actionTitle",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

                // Row 3: 6 Auto Action Checkboxes
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
                onClear = { viewModel.clearLogs() }
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
                onClear = { viewModel.clearLogs() }
            )
        }
    }
}

// =============================================================================
// 3️⃣ SERVICE / UNLOCK SCREEN TAB (SCROLLABLE GROUP BOX + CHECKBOX + START BUTTON)
// =============================================================================
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
    var selectedActionId by remember { mutableStateOf("srv_erase_frp") }

    val serviceGroups = remember {
        listOf(
            ActionGroup(
                title = "📱 CHIPSET & STORAGE GPT",
                icon = Icons.Default.Memory,
                items = listOf(
                    ActionItem("srv_read_info", "Read Chip Info & Registers", "Detect MediaTek HW Code, Security & DA state", Color(0xFF06B6D4)),
                    ActionItem("srv_read_rpmb", "Read RPMB Security Partition", "Dump RPMB keys and security region", Color(0xFF6366F1)),
                    ActionItem("srv_read_gpt", "Read GPT & Generate Scatter", "Query live storage GPT table and build scatter", Color(0xFF38BDF8))
                )
            ),
            ActionGroup(
                title = "🔓 SECURITY & UNLOCK OPERATIONS",
                icon = Icons.Default.LockOpen,
                items = listOf(
                    ActionItem("srv_erase_frp", "Erase FRP (Google Account Removal)", "Zero-out FRP partition via Direct USB BROM", Color(0xFFF59E0B)),
                    ActionItem("srv_unlock_bl", "Unlock Bootloader (seccfg patch)", "Write unlock payload to seccfg partition", Color(0xFF10B981)),
                    ActionItem("srv_lock_bl", "Lock Bootloader (seccfg restore)", "Relock bootloader security state", Color(0xFF64748B)),
                    ActionItem("srv_factory_reset", "Factory Reset (Wipe Userdata)", "Format userdata, metadata and cache partitions", Color(0xFFEF4444)),
                    ActionItem("srv_disable_mi", "Disable Mi Account (Xiaomi)", "Patch persist and frp to remove Mi Cloud lock", Color(0xFFF97316))
                )
            ),
            ActionGroup(
                title = "🛡️ NV DATA & CALIBRATION",
                icon = Icons.Default.Shield,
                items = listOf(
                    ActionItem("srv_backup_nv", "Backup NVRAM / NVDATA (IMEI Guard)", "Dump nvram, nvdata, protect1/2, persist to backup", Color(0xFF8B5CF6)),
                    ActionItem("srv_restore_nv", "Restore NVRAM / NVDATA", "Write back saved NV calibration archive", Color(0xFFA855F7))
                )
            ),
            ActionGroup(
                title = "⚡ BROM EXPLOITS & REBOOT",
                icon = Icons.Default.Refresh,
                items = listOf(
                    ActionItem("srv_crash_brom", "Crash Preloader to BROM", "Send USB Control Transfer command to force BROM", Color(0xFFEC4899)),
                    ActionItem("srv_reboot_system", "Reboot to Android System", "Send DA reboot command to boot Android OS", Color(0xFF38BDF8)),
                    ActionItem("srv_reboot_fastboot", "Reboot to Fastboot Mode", "Send DA reboot command to enter fastboot", Color(0xFF0284C7)),
                    ActionItem("srv_reboot_recovery", "Reboot to Recovery Mode", "Send DA reboot command to enter recovery", Color(0xFF6366F1))
                )
            )
        )
    }

    val selectedAction = serviceGroups.flatMap { it.items }.find { it.id == selectedActionId }

    fun executeSelectedService() {
        when (selectedActionId) {
            "srv_read_info" -> viewModel.executeServiceFunctionDirect(ServiceFunction.READ_INFO)
            "srv_read_rpmb" -> viewModel.executeServiceFunctionDirect(ServiceFunction.READ_RPMB)
            "srv_read_gpt" -> viewModel.executeServiceFunctionDirect(ServiceFunction.READ_GPT_SCATTER)
            "srv_erase_frp" -> viewModel.executeServiceFunctionDirect(ServiceFunction.ERASE_FRP)
            "srv_unlock_bl" -> viewModel.executeServiceFunctionDirect(ServiceFunction.UNLOCK_BOOTLOADER)
            "srv_lock_bl" -> viewModel.executeServiceFunctionDirect(ServiceFunction.LOCK_BOOTLOADER)
            "srv_factory_reset" -> viewModel.executeServiceFunctionDirect(ServiceFunction.FACTORY_RESET)
            "srv_disable_mi" -> viewModel.executeServiceFunctionDirect(ServiceFunction.DISABLE_MI_ACCOUNT)
            "srv_backup_nv" -> viewModel.executeServiceFunctionDirect(ServiceFunction.BACKUP_NVRAM)
            "srv_restore_nv" -> viewModel.executeServiceFunctionDirect(ServiceFunction.RESTORE_NVRAM)
            "srv_crash_brom" -> viewModel.executeServiceFunctionDirect(ServiceFunction.CRASH_TO_BROM)
            "srv_reboot_system" -> viewModel.executeServiceFunctionDirect(ServiceFunction.REBOOT_SYSTEM)
            "srv_reboot_fastboot" -> viewModel.executeServiceFunctionDirect(ServiceFunction.REBOOT_FASTBOOT)
            "srv_reboot_recovery" -> viewModel.executeServiceFunctionDirect(ServiceFunction.REBOOT_RECOVERY)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Brand / Model & Safety Row
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
            }
        }

        // 1. Scrollable Group Box with Checkbox selections
        ActionGroupSelectorBox(
            groups = serviceGroups,
            selectedId = selectedActionId,
            onSelectId = { selectedActionId = it },
            modifier = Modifier.weight(0.42f)
        )

        // 2. Single START Button
        SingleStartActionButton(
            actionTitle = selectedAction?.title ?: "Select Action",
            isRunning = progress.isRunning,
            enabled = !progress.isRunning,
            accentColor = selectedAction?.accentColor ?: Color(0xFF059669),
            onExecute = { executeSelectedService() },
            onStop = onStopClick
        )

        // 3. Spacious Live Terminal Log Box
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(0.58f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() }
            )
        }
    }
}

// =============================================================================
// 4️⃣ FASTBOOT SCREEN TAB (SCROLLABLE GROUP BOX + CHECKBOX + START BUTTON)
// =============================================================================
@Composable
private fun FastbootScreenTab(
    viewModel: MtkBridgeViewModel,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    val isFastbootBusy by viewModel.isFastbootBusy.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()
    var selectedActionId by remember { mutableStateOf("fb_getvar_all") }

    val fastbootGroups = remember {
        listOf(
            ActionGroup(
                title = "📋 IDENTIFICATION & VARIABLES",
                icon = Icons.Default.Memory,
                items = listOf(
                    ActionItem("fb_getvar_all", "Get All Variables (getvar:all)", "Read product, bootloader version & secure state", Color(0xFF38BDF8)),
                    ActionItem("fb_check_lock", "Check Bootloader Lock Status", "Query fastboot getvar:unlocked", Color(0xFF06B6D4)),
                    ActionItem("fb_battery", "Read Battery Voltage", "Query fastboot getvar:battery-voltage", Color(0xFF10B981))
                )
            ),
            ActionGroup(
                title = "🔓 BOOTLOADER & PARTITION UNLOCK",
                icon = Icons.Default.LockOpen,
                items = listOf(
                    ActionItem("fb_unlock_bl", "Flashing Unlock (Unlock Bootloader)", "Execute fastboot flashing unlock", Color(0xFF10B981)),
                    ActionItem("fb_lock_bl", "Flashing Lock (Relock Bootloader)", "Execute fastboot flashing lock", Color(0xFF64748B)),
                    ActionItem("fb_erase_frp", "Erase FRP Partition (Google Account)", "Execute fastboot erase:frp", Color(0xFFF59E0B)),
                    ActionItem("fb_format_userdata", "Format Userdata (Factory Wipe)", "Execute fastboot erase:userdata", Color(0xFFEF4444)),
                    ActionItem("fb_erase_metadata", "Erase Metadata Partition", "Execute fastboot erase:metadata", Color(0xFFE11D48))
                )
            ),
            ActionGroup(
                title = "🔄 REBOOT DESTINATIONS",
                icon = Icons.Default.Refresh,
                items = listOf(
                    ActionItem("fb_reboot_system", "Reboot to Android System", "Execute fastboot reboot", Color(0xFF38BDF8)),
                    ActionItem("fb_reboot_recovery", "Reboot to Recovery Mode", "Execute fastboot reboot-recovery", Color(0xFF6366F1)),
                    ActionItem("fb_reboot_fastbootd", "Reboot to FastbootD (Userspace)", "Execute fastboot reboot-fastboot", Color(0xFF8B5CF6)),
                    ActionItem("fb_reboot_edl", "Reboot to EDL / BROM Mode", "Execute fastboot oem edl", Color(0xFFEC4899))
                )
            )
        )
    }

    val selectedAction = fastbootGroups.flatMap { it.items }.find { it.id == selectedActionId }

    fun executeSelectedFastboot() {
        when (selectedActionId) {
            "fb_getvar_all" -> viewModel.runFastbootReadAllVars()
            "fb_check_lock" -> viewModel.runFastbootCommand("Check Lock Status", "getvar:unlocked")
            "fb_battery" -> viewModel.runFastbootCommand("Read Battery Voltage", "getvar:battery-voltage")
            "fb_unlock_bl" -> viewModel.runFastbootUnlockBootloader()
            "fb_lock_bl" -> viewModel.runFastbootLockBootloader()
            "fb_erase_frp" -> viewModel.runFastbootEraseFrp()
            "fb_format_userdata" -> viewModel.runFastbootFormatUserdata()
            "fb_erase_metadata" -> viewModel.runFastbootCommand("Erase Metadata", "erase:metadata")
            "fb_reboot_system" -> viewModel.runFastbootReboot("system")
            "fb_reboot_recovery" -> viewModel.runFastbootReboot("recovery")
            "fb_reboot_fastbootd" -> viewModel.runFastbootReboot("fastbootd")
            "fb_reboot_edl" -> viewModel.runFastbootReboot("edl")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status Bar
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                    Text("Fastboot Protocol Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                }
                Text(if (isDryRun) "SIMULATION ACTIVE" else "LIVE FASTBOOT", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
            }
        }

        // 1. Scrollable Group Box with Checkbox selections
        ActionGroupSelectorBox(
            groups = fastbootGroups,
            selectedId = selectedActionId,
            onSelectId = { selectedActionId = it },
            modifier = Modifier.weight(0.42f)
        )

        // 2. Single START Button
        SingleStartActionButton(
            actionTitle = selectedAction?.title ?: "Select Action",
            isRunning = isFastbootBusy,
            enabled = !isFastbootBusy,
            accentColor = selectedAction?.accentColor ?: Color(0xFF059669),
            onExecute = { executeSelectedFastboot() },
            onStop = onStopClick
        )

        // 3. Spacious Live Terminal Log Box
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(0.58f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() }
            )
        }
    }
}

// =============================================================================
// 5️⃣ ADB SCREEN TAB (SCROLLABLE GROUP BOX + CHECKBOX + START BUTTON)
// =============================================================================
@Composable
private fun AdbScreenTab(
    viewModel: MtkBridgeViewModel,
    progress: OperationProgress,
    logs: List<TerminalLog>,
    onStopClick: () -> Unit
) {
    val isAdbBusy by viewModel.isAdbBusy.collectAsState()
    val isDryRun by viewModel.isDryRun.collectAsState()
    var selectedActionId by remember { mutableStateOf("adb_read_info") }

    val adbGroups = remember {
        listOf(
            ActionGroup(
                title = "📱 DEVICE IDENTIFICATION & STATUS",
                icon = Icons.Default.Memory,
                items = listOf(
                    ActionItem("adb_read_info", "Read Full Device Info & OS Build", "getprop (Model, Brand, Android Release, Security Patch, Board)", Color(0xFF06B6D4)),
                    ActionItem("adb_battery", "Battery Health & Temp Diagnostic", "dumpsys battery (Level, Health, Temp, Voltage)", Color(0xFF10B981))
                )
            ),
            ActionGroup(
                title = "🔓 BYPASS & SERVICE OPERATIONS",
                icon = Icons.Default.LockOpen,
                items = listOf(
                    ActionItem("adb_bypass_frp", "Bypass Setup Wizard (FRP Bypass)", "Set device_provisioned flag & launch Android Home", Color(0xFFF59E0B)),
                    ActionItem("adb_enable_lang", "Enable All Languages (MoreLocale)", "Grant CHANGE_CONFIGURATION permission via ADB", Color(0xFF0D9488))
                )
            ),
            ActionGroup(
                title = "🗑️ BLOATWARE REMOVER (SYSTEM CLEANUP)",
                icon = Icons.Default.Clear,
                items = listOf(
                    ActionItem("adb_miui_bloat", "Disable Xiaomi / MIUI Bloatware", "Uninstall Analytics, MSA, CleanMaster (5 apps)", Color(0xFFF97316)),
                    ActionItem("adb_oppo_bloat", "Disable Oppo / Realme Bloatware", "Uninstall ThemeStore, Market, GameCenter (4 apps)", Color(0xFFEF4444))
                )
            ),
            ActionGroup(
                title = "🔄 REBOOT & MODE SWITCH",
                icon = Icons.Default.Refresh,
                items = listOf(
                    ActionItem("adb_reboot_system", "Reboot to Android System", "adb shell reboot", Color(0xFF38BDF8)),
                    ActionItem("adb_reboot_fastboot", "Reboot to Fastboot (Bootloader)", "adb shell reboot bootloader", Color(0xFF0284C7)),
                    ActionItem("adb_reboot_recovery", "Reboot to Recovery Mode", "adb shell reboot recovery", Color(0xFF6366F1)),
                    ActionItem("adb_reboot_edl", "Reboot to BROM / EDL Mode", "adb shell reboot edl", Color(0xFFEC4899))
                )
            )
        )
    }

    val selectedAction = adbGroups.flatMap { it.items }.find { it.id == selectedActionId }

    fun executeSelectedAdb() {
        when (selectedActionId) {
            "adb_read_info" -> viewModel.runAdbReadInfo()
            "adb_battery" -> viewModel.runAdbCommand("Battery Info", "dumpsys battery | grep -E 'level|status|health|temperature'")
            "adb_bypass_frp" -> viewModel.runAdbBypassFrp()
            "adb_enable_lang" -> viewModel.runAdbEnableLanguages()
            "adb_miui_bloat" -> {
                val miBloat = listOf(
                    "com.miui.analytics", "com.miui.msa.global", "com.xiaomi.glgm",
                    "com.miui.bugreport", "com.miui.cleanmaster"
                )
                viewModel.runAdbRemoveBloatware(miBloat)
            }
            "adb_oppo_bloat" -> {
                val oppoBloat = listOf(
                    "com.heytap.mcs", "com.heytap.themestore", "com.oppo.market",
                    "com.nearme.gamecenter"
                )
                viewModel.runAdbRemoveBloatware(oppoBloat)
            }
            "adb_reboot_system" -> viewModel.runAdbReboot("system")
            "adb_reboot_fastboot" -> viewModel.runAdbReboot("bootloader")
            "adb_reboot_recovery" -> viewModel.runAdbReboot("recovery")
            "adb_reboot_edl" -> viewModel.runAdbReboot("edl")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status Bar
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                    Text("Android Debug Bridge (ADB Host)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                }
                Text(if (isDryRun) "SIMULATION ACTIVE" else "LIVE ADB HOST", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
            }
        }

        // 1. Scrollable Group Box with Checkbox selections
        ActionGroupSelectorBox(
            groups = adbGroups,
            selectedId = selectedActionId,
            onSelectId = { selectedActionId = it },
            modifier = Modifier.weight(0.42f)
        )

        // 2. Single START Button
        SingleStartActionButton(
            actionTitle = selectedAction?.title ?: "Select Action",
            isRunning = isAdbBusy,
            enabled = !isAdbBusy,
            accentColor = selectedAction?.accentColor ?: Color(0xFF059669),
            onExecute = { executeSelectedAdb() },
            onStop = onStopClick
        )

        // 3. Spacious Live Terminal Log Box
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(0.58f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() }
            )
        }
    }
}

// =============================================================================
// 6️⃣ OTHER & ADVANCED SCREEN TAB (SCROLLABLE GROUP BOX + CHECKBOX + START BUTTON)
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
    var selectedActionId by remember { mutableStateOf("adv_mem_test") }

    val advancedGroups = remember {
        listOf(
            ActionGroup(
                title = "🧪 HARDWARE DIAGNOSTICS & MEMORY",
                icon = Icons.Default.Memory,
                items = listOf(
                    ActionItem("adv_mem_test", "RAM & eMMC/UFS Memory Diagnostic", "Perform RAM pattern test, storage health & CID diagnostic", Color(0xFF06B6D4)),
                    ActionItem("adv_read_preloader", "Read Preloader & LK Bootloader", "Dump preloader.bin and lk bootloader images", Color(0xFF38BDF8))
                )
            ),
            ActionGroup(
                title = "🛡️ SECURITY & AUTH EXPLOITS",
                icon = Icons.Default.Security,
                items = listOf(
                    ActionItem("adv_auth_bypass", "SLA / DAA Bypass (Kamakiri Exploit)", "Execute USB Control Transfer exploit to disable SLA/DAA security", Color(0xFFF43F5E)),
                    ActionItem("adv_watchdog", "USB Watchdog Reset Control", "Send USB Control Transfer Watchdog Reset", Color(0xFF3B82F6)),
                    ActionItem("adv_crash_brom", "Force Crash to BROM", "Force preloader handshake down to BROM mode", Color(0xFFEC4899))
                )
            )
        )
    }

    val selectedAction = advancedGroups.flatMap { it.items }.find { it.id == selectedActionId }

    fun executeSelectedAdvanced() {
        when (selectedActionId) {
            "adv_mem_test" -> viewModel.runMemoryTest()
            "adv_read_preloader" -> viewModel.executeServiceFunctionDirect(ServiceFunction.READ_PRELOADER)
            "adv_auth_bypass" -> viewModel.executeServiceFunctionDirect(ServiceFunction.BYPASS_AUTH)
            "adv_watchdog" -> viewModel.sendWatchdogReset()
            "adv_crash_brom" -> viewModel.executeServiceFunctionDirect(ServiceFunction.CRASH_TO_BROM)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Brand / Model selector
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                BrandModelSelectorRow(
                    selectedBrand = selectedBrand,
                    selectedModel = selectedModel,
                    onBrandSelect = { viewModel.selectBrand(it) },
                    onModelSelect = { viewModel.selectModel(it) }
                )
            }
        }

        // 1. Scrollable Group Box with Checkbox selections
        ActionGroupSelectorBox(
            groups = advancedGroups,
            selectedId = selectedActionId,
            onSelectId = { selectedActionId = it },
            modifier = Modifier.weight(0.42f)
        )

        // 2. Single START Button
        SingleStartActionButton(
            actionTitle = selectedAction?.title ?: "Select Action",
            isRunning = progress.isRunning,
            enabled = !progress.isRunning,
            accentColor = selectedAction?.accentColor ?: Color(0xFF059669),
            onExecute = { executeSelectedAdvanced() },
            onStop = onStopClick
        )

        // 3. Spacious Live Terminal Log Box
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().weight(0.58f)
        ) {
            CompactTerminalLogView(
                logs = logs,
                onClear = { viewModel.clearLogs() }
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
// COMPACT TERMINAL LOG VIEW (MAXIMUM USABLE HEIGHT & SPATIAL POLISH)
// =============================================================================
@Composable
private fun CompactTerminalLogView(
    logs: List<TerminalLog>,
    onClear: () -> Unit
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
                    String.format(Locale.US, "%.1f MB/s", progress.speedKbPerSec / 1024.0)
                } else {
                    String.format(Locale.US, "%.0f KB/s", progress.speedKbPerSec)
                }

                if (progress.isRunning && progress.speedKbPerSec > 0) {
                    Text(
                        text = "$speedText | ETA: ${progress.estimatedSecondsRemaining}s",
                        fontSize = 8.5.sp,
                        color = Color(0xFF4ADE80),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (progress.isRunning) {
                LinearProgressIndicator(
                    progress = { progress.percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B),
                )
            }
        }
    }
}

private fun nowTime(): String {
    return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
}
