package com.aashutarminal.bootstrap

import android.content.Context
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Handles the mechanics of laying out $PREFIX and extracting the base
 * rootfs archive. The archive itself (bootstrap-<arch>.zip, containing a
 * real busybox + bash + coreutils userland) is fetched from Termux's own
 * official GitHub releases at BUILD time by the `downloadBootstraps`
 * Gradle task in app/build.gradle -- see that file for details. This
 * class only extracts whatever bootstrap-<arch>.zip already ships in
 * assets/bootstrap/.
 */
class EnvironmentSetup(
    private val context: Context,
    private val prefixDir: File,
    private val homeDir: File
) {
    /** Maps Android's ABI name to Termux's bootstrap archive arch name. */
    private fun termuxArchFor(abi: String): String = when (abi) {
        "arm64-v8a" -> "aarch64"
        "armeabi-v7a" -> "arm"
        "x86_64" -> "x86_64"
        "x86" -> "i686"
        else -> "aarch64"
    }

    fun createDirLayout() {
        listOf("bin", "lib", "etc", "tmp", "var", "share").forEach {
            File(prefixDir, it).mkdirs()
        }
    }

    fun extractBaseRootfs() {
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val arch = termuxArchFor(abi)
        val assetName = "bootstrap/bootstrap-$arch.zip"

        val availableAssets = runCatching { context.assets.list("bootstrap")?.toList() ?: emptyList() }
            .getOrElse { emptyList() }

        if (assetName.removePrefix("bootstrap/") !in availableAssets) {
            throw IllegalStateException(
                "Expected asset '$assetName' not found. Files actually bundled " +
                    "under assets/bootstrap/: ${availableAssets.ifEmpty { listOf("(none)") }}"
            )
        }

        // NOTE: deliberately using a single context.assets.open() call for
        // everything below. Mixing this with a separate assets.openFd()
        // call (e.g. for an upfront size check) can cause "Stream closed"
        // errors, since AssetManager can share underlying file descriptors
        // across calls for the same asset path -- closing one closes both.
        var filesWritten = 0
        var totalBytes = 0L
        context.assets.open(assetName).use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    // Termux bootstrap zips store symlinks as a special
                    // "SYMLINKS.txt" manifest rather than real zip
                    // symlink entries (zip has no native symlink
                    // support). Handle that file specially; everything
                    // else extracts as a normal file.
                    if (entry.name == "SYMLINKS.txt") {
                        val text = zis.readBytes().toString(Charsets.UTF_8)
                        text.lineSequence().forEach { line ->
                            if (line.isBlank()) return@forEach
                            val parts = line.split("←", "<-").map { it.trim() }
                            if (parts.size == 2) {
                                val target = File(prefixDir, parts[1])
                                target.parentFile?.mkdirs()
                                runCatching {
                                    android.system.Os.symlink(parts[0], target.absolutePath)
                                }
                            }
                        }
                    } else if (!entry.isDirectory) {
                        val outFile = File(prefixDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        val bytes = zis.readBytes()
                        outFile.writeBytes(bytes)
                        outFile.setExecutable(true)
                        filesWritten++
                        totalBytes += bytes.size
                    } else {
                        File(prefixDir, entry.name).mkdirs()
                    }
                    entry = zis.nextEntry
                }
            }
        }

        if (filesWritten == 0) {
            throw IllegalStateException("Zip '$assetName' opened but contained 0 extractable files.")
        }
        if (totalBytes < 1_000_000L) {
            // A real Termux bootstrap is tens of MB; a few KB means we
            // extracted an error page or truncated archive, not the
            // genuine userland.
            throw IllegalStateException(
                "Zip '$assetName' extracted only ${totalBytes} bytes across $filesWritten files -- " +
                    "too small to be a real bootstrap archive."
            )
        }
    }

    fun writeDefaultConfig() {
        File(homeDir, ".bashrc").writeText(
            """
            export PS1='\[\e[36m\]\w\[\e[0m\] $ '
            export EDITOR=nano
            alias ll='ls -la'
            """.trimIndent()
        )
        runCatching {
            context.assets.open("bootstrap/welcome.txt").use { input ->
                File(homeDir, ".motd").outputStream().use { input.copyTo(it) }
            }
        }
    }
}
