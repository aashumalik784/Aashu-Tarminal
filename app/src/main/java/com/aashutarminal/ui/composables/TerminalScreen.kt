package com.aashutarminal.ui.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.aashutarminal.terminal.TerminalSession
import com.aashutarminal.terminal.TerminalView

@Composable
fun TerminalScreen(session: TerminalSession) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            TerminalView(context).apply { this.session = session }
        }
    )
}
