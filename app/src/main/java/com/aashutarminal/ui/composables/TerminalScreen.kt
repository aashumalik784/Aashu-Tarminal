package com.aashutarminal.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aashutarminal.terminal.TerminalSession
import com.aashutarminal.terminal.TerminalView

/**
 * Termux-style layout: the terminal view fills the screen, with a
 * horizontally scrollable row of extra keys (ESC, TAB, CTRL, arrows, and
 * common shell symbols) pinned above the soft keyboard.
 */
@Composable
fun TerminalScreen(session: TerminalSession) {
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                TerminalView(context).apply {
                    this.session = session
                    terminalView = this
                }
            }
        )
        ExtraKeysRow(onKey = { bytes -> terminalView?.sendRaw(bytes) })
    }
}

private data class ExtraKey(val label: String, val bytes: ByteArray)

@Composable
private fun ExtraKeysRow(onKey: (ByteArray) -> Unit) {
    val keys = listOf(
        ExtraKey("ESC", byteArrayOf(27)),
        ExtraKey("TAB", byteArrayOf(9)),
        ExtraKey("CTRL-C", byteArrayOf(3)),
        ExtraKey("/", "/".toByteArray()),
        ExtraKey("-", "-".toByteArray()),
        ExtraKey("|", "|".toByteArray()),
        ExtraKey("~", "~".toByteArray()),
        ExtraKey("↑", byteArrayOf(27, '['.code.toByte(), 'A'.code.toByte())),
        ExtraKey("↓", byteArrayOf(27, '['.code.toByte(), 'B'.code.toByte())),
        ExtraKey("←", byteArrayOf(27, '['.code.toByte(), 'D'.code.toByte())),
        ExtraKey("→", byteArrayOf(27, '['.code.toByte(), 'C'.code.toByte())),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22))
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        keys.forEach { key ->
            OutlinedButton(
                onClick = { onKey(key.bytes) },
                modifier = Modifier.padding(horizontal = 2.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(key.label, fontSize = 13.sp)
            }
        }
    }
}
