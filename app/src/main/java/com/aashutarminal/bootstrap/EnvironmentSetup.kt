package com.aashutarminal.bootstrap

import android.content.Context
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Handles the mechanics of laying out $PREFIX and extracting the base
 * rootfs archive bundled in assets (or downloaded on demand for smaller
 * APK size — see ToolInstaller for the download path).
 */
class EnvironmentSetup(
    private val context: Context,
    private val prefixDir: File,
    private val homeDir: File
) {
    fun createDirLayout() {
        listOf("bin", "lib", "etc", "tmp", "var", "share").forEach {
            File(prefixDir, it).mkdirs()
        }
    }

    fun extractBaseRootfs() {
        // Base rootfs ships as assets/bootstrap/rootfs-<abi>.zip (not
        // included in this scaffold — populate via CI before release).
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val assetName = "bootstrap/rootfs-$abi.zip"
        runCatching {
            context.assets.open(assetName).use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(prefixDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { zis.copyTo(it) }
                            outFile.setExecutable(true)
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        }
    }

    fun writeDefaultConfig() {
        File(homeDir, ".bashrc").writeText(
            """
            export PS1='\w $ '
            alias ll='ls -la'
            """.trimIndent()
        )
        context.assets.open("bootstrap/welcome.txt").use { input ->
            File(homeDir, ".motd").outputStream().use { input.copyTo(it) }
        }
    }
}
