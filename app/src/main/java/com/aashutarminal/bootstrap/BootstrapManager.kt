package com.aashutarminal.bootstrap

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Aashu Bootstrap — custom, from-scratch bootstrap system (not derived
 * from any other project). Lays down a minimal Linux userland under the
 * app's private storage and exposes the environment needed to spawn a
 * login shell.
 *
 * Android 10+ blocks executing files from an app's own writable storage,
 * so the actual bash/busybox binaries can't run straight out of $PREFIX.
 * Instead, app/build.gradle's `repackageBootstrapExecs` task copies every
 * executable into the APK's native-library directory (the one place
 * Android grants exec permission) under a sanitized libtx_*.so name, and
 * writes a JSON manifest (assets/bootstrap/exec-map-<arch>.json) mapping
 * original relative paths ("bin/bash") to those names. This class resolves
 * the shell through that manifest, and an LD_PRELOAD shim (exec_shim.cpp,
 * built as libaashuexec.so) applies the same redirect to every command
 * the shell itself runs afterwards.
 */
class BootstrapManager(private val context: Context) {

    private val prefixDir: File get() = File(context.filesDir, "usr")
    private val homeDir: File get() = File(context.filesDir, "home")
    private val nativeLibDir: String get() = context.applicationInfo.nativeLibraryDir

    private fun termuxArchFor(abi: String): String = when (abi) {
        "arm64-v8a" -> "aarch64"
        "armeabi-v7a" -> "arm"
        "x86_64" -> "x86_64"
        "x86" -> "i686"
        else -> "aarch64"
    }

    private fun currentArch(): String =
        termuxArchFor(android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")

    /** original relative path ("bin/bash") -> jniLibs filename ("libtx_bin_bash.so") */
    private fun loadExecMap(): Map<String, String> = runCatching {
        val text = context.assets.open("bootstrap/exec-map-${currentArch()}.json")
            .bufferedReader().readText()
        val json = JSONObject(text)
        json.keys().asSequence().associateWith { json.getString(it) }
    }.getOrDefault(emptyMap())

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
                progress(0.5f)
                writeExecMapTsv(loadExecMap())
                progress(0.7f)
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

    /**
     * Finds the real, exec-permitted path for our shell: the jniLibs copy
     * of bash/sh (via the exec map), not the (non-executable) extracted
     * copy under $PREFIX.
     */
    private fun resolveShell(): File? {
        val map = loadExecMap()
        val candidates = listOf("bin/bash", "bin/sh", "bin/busybox")
        for (rel in candidates) {
            val soName = map[rel] ?: continue
            val real = File(nativeLibDir, soName)
            if (real.exists() && real.canExecute()) return real
        }
        return null
    }

    fun buildSessionEnvironment(): SessionEnv {
        // Fall back to Android's own built-in shell if our bootstrap isn't
        // present -- this guarantees the terminal always shows *something*
        // usable instead of a silent blank screen.
        val resolved = resolveShell()
        val shell = resolved?.absolutePath ?: "/system/bin/sh"
        return SessionEnv(
            shellPath = shell,
            homeDir = homeDir.absolutePath,
            prefixDir = prefixDir.absolutePath,
            nativeLibDir = nativeLibDir,
            usingFallbackShell = resolved == null
        )
    }

    data class SessionEnv(
        val shellPath: String,
        val homeDir: String,
        val prefixDir: String,
        val nativeLibDir: String,
        val usingFallbackShell: Boolean = false
    ) {
        fun toEnvArray(): Array<String> {
            val base = mutableListOf(
                "HOME=$homeDir",
                "PREFIX=$prefixDir",
                "PATH=$prefixDir/bin:/system/bin",
                "LD_LIBRARY_PATH=$prefixDir/lib",
                "TERM=xterm-256color"
            )
            if (!usingFallbackShell) {
                // Consumed by exec_shim.cpp (libaashuexec.so) to redirect
                // every command the shell runs to its jniLibs equivalent.
                base += "AASHU_NATIVE_LIB_DIR=$nativeLibDir"
                base += "AASHU_EXEC_MAP_TSV=$prefixDir/etc/exec-map.tsv"
                base += "LD_PRELOAD=$nativeLibDir/libaashuexec.so"
            }
            return base.toTypedArray()
        }
    }
}
