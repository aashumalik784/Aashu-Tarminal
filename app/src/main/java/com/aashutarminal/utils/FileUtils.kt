package com.aashutarminal.utils

import java.io.File

object FileUtils {
    fun sizeOf(dir: File): Long =
        if (dir.isFile) dir.length()
        else dir.listFiles()?.sumOf { sizeOf(it) } ?: 0L

    fun humanReadableSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB")
        var value = bytes / 1024.0
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return "%.1f %s".format(value, units[unitIndex])
    }

    fun ensureDir(dir: File): File = dir.apply { mkdirs() }
}
