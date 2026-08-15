package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.model.TerminalLog
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalPrompt
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TerminalTimestamp
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun TerminalLogViewer(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Auto scroll to bottom on new log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Console Log",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5. Live Console & Diagnostics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            val allText = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                            clipboardManager.setText(AnnotatedString(allText))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Logs", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Dark Terminal Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 280.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalBackground)
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Console ready. No logs generated yet.",
                        fontSize = 11.sp,
                        color = TerminalTimestamp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { log ->
                            LogRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(log: TerminalLog) {
    val levelColor = when (log.level) {
        LogLevel.SUCCESS -> StatusSuccess
        LogLevel.ERROR -> StatusError
        LogLevel.WARNING -> StatusWarning
        LogLevel.AI -> Color(0xFF818CF8)
        LogLevel.RAW -> Color(0xFF94A3B8)
        LogLevel.INFO -> TerminalText
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${log.timestamp} ",
            color = TerminalTimestamp,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
        Text(
            text = log.message,
            color = levelColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}
