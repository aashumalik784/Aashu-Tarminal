package com.aashutarminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aashutarminal.terminal.TerminalSession
import com.aashutarminal.ui.composables.TerminalScreen
import com.aashutarminal.ui.theme.AashuTarminalTheme

/**
 * Hosts a single terminal session and its Compose-based view.
 */
class TerminalActivity : ComponentActivity() {

    private lateinit var session: TerminalSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AashuApp
        val env = app.bootstrapManager.buildSessionEnvironment()

        session = TerminalSession(
            shellPath = env.shellPath,
            args = emptyArray(),
            cwd = env.homeDir,
            envVars = env.toEnvArray()
        )
        session.start()

        setContent {
            AashuTarminalTheme {
                TerminalScreen(session = session)
            }
        }
    }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
    }
}
