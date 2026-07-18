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

        val assetSize = runCatching {
            context.assets.openFd(assetName).use { it.length }
        }.getOrDefault(-1L)
        if (assetSize < 1000L) {
            throw IllegalStateException(
                "Asset '$assetName' exists but is suspiciously small (${assetSize} bytes) -- " +
                    "the build-time download likely failed or fetched an error page instead of the real archive."
            )
        }

        context.assets.open(assetName).use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                var filesWritten = 0
                while (entry != null) {
                    // Termux bootstrap zips store symlinks as a special
                    // "SYMLINKS.txt" manifest rather than real zip
                    // symlink entries (zip has no native symlink
                    // support). Handle that file specially; everything
                    // else extracts as a normal file.
                    if (entry.name == "SYMLINKS.txt") {
                        zis.bufferedReader().readLines().forEach { line ->
                            val parts = line.split("←", "<-").map { it.trim() }
                            if (parts.size == 2) {
                                val target = File(prefixDir, parts[1])
                                target.parentFile?.mkdirs()
                                runCatching {
                                    android.system.Os.symlink(parts[0], target.absolutePath)
                                }
                            }
                        }
                    } else {
                        val outFile = File(prefixDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { zis.copyTo(it) }
                            outFile.setExecutable(true)
                            filesWritten++
                        }
                    }
                    entry = zis.nextEntry
                }
                if (filesWritten == 0) {
                    throw IllegalStateException("Zip '$assetName' opened but contained 0 extractable files.")
                }
            }
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
