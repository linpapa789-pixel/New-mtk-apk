package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
import com.example.model.ServiceFunction
import com.example.model.TerminalLog
import com.example.ui.theme.MtkBorderLight
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TerminalTimestamp
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun ConsoleAiScreen(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var isCompactFont by remember { mutableStateOf(true) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pure Terminal Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MTK Terminal Monitor",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF334155)
                    ) {
                        Text(
                            text = "${logs.size} lines",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Font size toggle
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCompactFont) Color(0xFF2563EB) else Color(0xFF334155),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = if (isCompactFont) "Small 10pt" else "Med 12pt",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { isCompactFont = !isCompactFont },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = "A/a",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    IconButton(
                        onClick = {
                            val allText = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                            clipboardManager.setText(AnnotatedString(allText))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Pristine Monospace Terminal Window (Full Canvas)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF030712),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "MTK Client Console initialized. Connect device or run command to start...",
                        fontSize = if (isCompactFont) 10.sp else 12.sp,
                        color = Color(0xFF475569),
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logs) { log ->
                            TerminalLogRow(log = log, isCompact = isCompactFont)
                        }
                    }
                }
            }
        }

        // Quick MTK Command Action Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    viewModel.selectServiceFunction(ServiceFunction.READ_INFO)
                    viewModel.executeActiveServiceFunction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Read Device Info", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.selectServiceFunction(ServiceFunction.BYPASS_AUTH)
                    viewModel.executeActiveServiceFunction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bypass Auth (SLA/DAA)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.selectServiceFunction(ServiceFunction.UNLOCK_BOOTLOADER)
                    viewModel.executeActiveServiceFunction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Unlock Bootloader", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.selectServiceFunction(ServiceFunction.ERASE_FRP)
                    viewModel.executeActiveServiceFunction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Erase FRP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.selectServiceFunction(ServiceFunction.REBOOT_SYSTEM)
                    viewModel.executeActiveServiceFunction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reboot Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TerminalLogRow(log: TerminalLog, isCompact: Boolean) {
    val fontSize = if (isCompact) 10.sp else 12.sp
    val lineHeight = if (isCompact) 13.sp else 15.sp

    val color = when (log.level) {
        LogLevel.SUCCESS -> Color(0xFF4ADE80)
        LogLevel.ERROR -> Color(0xFFF87171)
        LogLevel.WARNING -> Color(0xFFFBBF24)
        LogLevel.RAW -> Color(0xFF94A3B8)
        LogLevel.AI -> Color(0xFF818CF8)
        LogLevel.INFO -> Color(0xFFE2E8F0)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${log.timestamp} ",
            color = Color(0xFF64748B),
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            lineHeight = lineHeight
        )
        Text(
            text = log.message,
            color = color,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            lineHeight = lineHeight
        )
    }
}
