package com.aashutarminal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashutarminal.ui.theme.AashuTarminalTheme

/**
 * Landing screen: shows bootstrap status and lets the user jump into a
 * terminal session or settings.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AashuApp

        setContent {
            AashuTarminalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var bootstrapped by remember {
                        mutableStateOf(app.bootstrapManager.isBootstrapped())
                    }
                    var bootstrapError by remember { mutableStateOf<String?>(null) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aashu Tarminal", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(16.dp))

                        if (!bootstrapped) {
                            Text("First-time setup required")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                bootstrapError = app.bootstrapManager.runBootstrap { }
                                bootstrapped = app.bootstrapManager.isBootstrapped()
                            }) {
                                Text("Run bootstrap")
                            }
                            bootstrapError?.let { err ->
                                Spacer(Modifier.height(12.dp))
                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                startActivity(Intent(this@MainActivity, TerminalActivity::class.java))
                            }) {
                                Text("Open terminal anyway (limited shell)")
                            }
                        } else {
                            Button(onClick = {
                                startActivity(Intent(this@MainActivity, TerminalActivity::class.java))
                            }) {
                                Text("Open terminal")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            }) {
                                Text("Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}
