package com.aashutarminal.bootstrap

import android.content.Context
import com.aashutarminal.tools.Tool
import java.io.File
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads and installs an individual tool package into $PREFIX,
 * verifying its checksum before extraction.
 */
class ToolInstaller(private val context: Context, private val prefixDir: File) {

    fun install(tool: Tool, onProgress: (Float) -> Unit = {}): Result<Unit> = runCatching {
        val tmpFile = File(context.cacheDir, "${tool.name}-${tool.version}.tar.gz")
        URL(tool.downloadUrl).openStream().use { input ->
            tmpFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        require(sha256(tmpFile) == tool.sha256) { "Checksum mismatch for ${tool.name}" }
        onProgress(0.6f)

        extractTarGz(tmpFile, prefixDir)
        tmpFile.delete()
        onProgress(1.0f)
    }

    fun remove(tool: Tool): Result<Unit> = runCatching {
        File(prefixDir, "bin/${tool.binary}").delete()
        // A real implementation tracks per-tool manifests to remove all
        // installed files/libs, not just the binary symlink.
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractTarGz(archive: File, destDir: File) {
        // Delegate to the `tar` binary from the bootstrapped userland once
        // available; a pure-Kotlin tar/gzip reader can replace this later.
        ProcessBuilder("tar", "-xzf", archive.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true)
            .start()
            .waitFor()
    }
}
