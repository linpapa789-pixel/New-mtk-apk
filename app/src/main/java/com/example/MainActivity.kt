package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNavDestination
import com.example.protocol.TargetPhoneState
import com.example.ui.components.AiDiagnosticDialog
import com.example.ui.screens.ConsoleAiScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.Esp32BridgeScreen
import com.example.ui.screens.ScatterFlashScreen
import com.example.ui.screens.ServiceToolsScreen
import com.example.ui.screens.UnlockToolFlashScreen
import com.example.ui.theme.MtkBorderLight
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MtkBridgeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MtkBridgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MtkMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MtkMainApp(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(AppNavDestination.PARTITION_FLASH) }

    val isDryRun by viewModel.isDryRun.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val chipInfo by viewModel.chipInfo.collectAsState()
    val targetPhoneState by viewModel.targetPhoneState.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.width(310.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1D4ED8))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MTK Flash Tool",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "ESP32-S3 Flash Controller",
                                fontSize = 11.sp,
                                color = Color(0xFFBFDBFE)
                            )
                        }

                        // Theme Accent & Light mode icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* Accent picker */ }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Palette, contentDescription = "Accent Picker", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { /* Dark toggle */ }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.WbSunny, contentDescription = "Theme Toggle", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Device & Bridge Status Pill in Header
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chipInfo.chipIdHex,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            val isBrom = targetPhoneState is TargetPhoneState.Connected || isDryRun
                            Text(
                                text = if (isBrom) "BROM Ready" else "Waiting Device",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBrom) Color(0xFF4ADE80) else Color(0xFFFDE047)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Menu items: "Partition Flash", "NV Data Backup/Restore", "Read Chip Info", "Serial/Log Monitor", "DA / Preloader / Scatter Manager", "Settings"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    AppNavDestination.values().forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                currentDestination = destination
                                scope.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFFEFF6FF),
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(color = MtkBorderLight)

                // Bottom of drawer: "Privacy Options" / "About" link + Dry Run switch
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dry-Run Safety Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text(if (isDryRun) "Safe emulation active" else "Real hardware flash", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = isDryRun,
                            onCheckedChange = { viewModel.toggleDryRun(it) },
                            modifier = Modifier.size(32.dp),
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1D4ED8))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAboutDialog = true }) {
                            Text("Privacy Options", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        TextButton(onClick = { showAboutDialog = true }) {
                            Text("About MTK Tool", fontSize = 11.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) {
        // FULL SCREEN without bulky TopAppBar Header
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .statusBarsPadding()
                .testTag("mtk_app_scaffold")
        ) {
            Crossfade(targetState = currentDestination, label = "screen_transition") { destination ->
                when (destination) {
                    AppNavDestination.PARTITION_FLASH -> UnlockToolFlashScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                    AppNavDestination.NV_BACKUP_RESTORE -> ServiceToolsScreen(viewModel = viewModel)
                    AppNavDestination.READ_CHIP_INFO -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = {
                            currentDestination = AppNavDestination.PARTITION_FLASH
                        }
                    )
                    AppNavDestination.SERIAL_LOG_MONITOR -> ConsoleAiScreen(viewModel = viewModel)
                    AppNavDestination.DA_SCATTER_MANAGER -> ScatterFlashScreen(viewModel = viewModel)
                    AppNavDestination.SETTINGS -> Esp32BridgeScreen(viewModel = viewModel)
                }
            }
        }
    }

    // AI Diagnostics Modal
    aiAnalysis?.let { analysisText ->
        AiDiagnosticDialog(
            analysisText = analysisText,
            onDismiss = { viewModel.dismissAiSheet() }
        )
    }

    // About & Privacy Dialog
    if (showAboutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About MTK Flash Bridge Tool", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version 2.4.0 (Pro GSM Flasher Edition)", fontSize = 12.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                    Text("Universal MediaTek BROM/Preloader Flashing utility with ESP32-S3 Hardware Trigger support (USB-CDC & Wi-Fi SoftAP).", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("• Auto-Detect Chipset & Scatter Autoloading\n• NVRAM & IMEI Data Dump/Restore Engine\n• Hardware Test-Point Pulse Triggering\n• Offline Dry-Run Simulation Safety", fontSize = 11.sp, color = Color(0xFF334155))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
