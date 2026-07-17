package com.aashutarminal.bootstrap

import android.content.Context
import java.io.File

/**
 * Aashu Bootstrap — custom, from-scratch bootstrap system (not derived
 * from any other project). Lays down a minimal Linux userland under the
 * app's private storage and exposes the environment needed to spawn a
 * login shell.
 */
class BootstrapManager(private val context: Context) {

    private val prefixDir: File get() = File(context.filesDir, "usr")
    private val homeDir: File get() = File(context.filesDir, "home")

    fun isBootstrapped(): Boolean =
        File(prefixDir, "bin/sh").exists() || File(prefixDir, ".bootstrap_done").exists()

    fun runBootstrap(progress: (Float) -> Unit) {
        prefixDir.mkdirs()
        homeDir.mkdirs()

        EnvironmentSetup(context, prefixDir, homeDir).apply {
            createDirLayout()
            progress(0.2f)
            extractBaseRootfs()
            progress(0.6f)
            writeDefaultConfig()
            progress(0.9f)
        }

        File(prefixDir, ".bootstrap_done").writeText("ok")
        progress(1.0f)
    }

    fun reset() {
        prefixDir.deleteRecursively()
        homeDir.deleteRecursively()
    }

    fun buildSessionEnvironment(): SessionEnv = SessionEnv(
        shellPath = File(prefixDir, "bin/bash").let {
            if (it.exists()) it.absolutePath else File(prefixDir, "bin/sh").absolutePath
        },
        homeDir = homeDir.absolutePath,
        prefixDir = prefixDir.absolutePath
    )

    data class SessionEnv(val shellPath: String, val homeDir: String, val prefixDir: String) {
        fun toEnvArray(): Array<String> = arrayOf(
            "HOME=$homeDir",
            "PREFIX=$prefixDir",
            "PATH=$prefixDir/bin",
            "LD_LIBRARY_PATH=$prefixDir/lib",
            "TERM=xterm-256color"
        )
    }
}
