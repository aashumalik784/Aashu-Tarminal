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

    /** True only if a real, executable shell binary was actually extracted. */
    fun isBootstrapped(): Boolean = resolveShell() != null

    /**
     * Runs bootstrap and returns a human-readable result: null on success,
     * or an error message describing what went wrong (e.g. no network,
     * bad archive) so the UI can show it instead of silently doing nothing.
     */
    fun runBootstrap(progress: (Float) -> Unit): String? {
        prefixDir.mkdirs()
        homeDir.mkdirs()

        return try {
            EnvironmentSetup(context, prefixDir, homeDir).apply {
                createDirLayout()
                progress(0.2f)
                extractBaseRootfs()
                progress(0.6f)
                writeDefaultConfig()
                progress(0.9f)
            }

            if (resolveShell() == null) {
                "Bootstrap ran but no shell binary was found afterwards. " +
                    "The bundled bootstrap-<arch>.zip in assets/bootstrap/ may be " +
                    "missing or for the wrong architecture (device ABI: " +
                    "${android.os.Build.SUPPORTED_ABIS.firstOrNull()})."
            } else {
                File(prefixDir, ".bootstrap_done").writeText("ok")
                progress(1.0f)
                null
            }
        } catch (t: Throwable) {
            "Bootstrap failed: ${t.message}"
        }
    }

    fun reset() {
        prefixDir.deleteRecursively()
        homeDir.deleteRecursively()
    }

    /** Finds a real, executable shell: our bootstrapped bash/sh first, else null. */
    private fun resolveShell(): File? {
        val candidates = listOf(
            File(prefixDir, "bin/bash"),
            File(prefixDir, "bin/sh"),
            File(prefixDir, "bin/busybox")
        )
        return candidates.firstOrNull { it.exists() && it.canExecute() }
    }

    fun buildSessionEnvironment(): SessionEnv {
        // Fall back to Android's own built-in shell if our bootstrap isn't
        // present -- this guarantees the terminal always shows *something*
        // usable instead of a silent blank screen.
        val shell = resolveShell()?.absolutePath ?: "/system/bin/sh"
        return SessionEnv(
            shellPath = shell,
            homeDir = homeDir.absolutePath,
            prefixDir = prefixDir.absolutePath,
            usingFallbackShell = resolveShell() == null
        )
    }

    data class SessionEnv(
        val shellPath: String,
        val homeDir: String,
        val prefixDir: String,
        val usingFallbackShell: Boolean = false
    ) {
        fun toEnvArray(): Array<String> = arrayOf(
            "HOME=$homeDir",
            "PREFIX=$prefixDir",
            "PATH=$prefixDir/bin:/system/bin",
            "LD_LIBRARY_PATH=$prefixDir/lib",
            "TERM=xterm-256color"
        )
    }
}
