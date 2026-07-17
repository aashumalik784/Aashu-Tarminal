package com.aashutarminal.tools

import android.content.Context
import com.aashutarminal.bootstrap.ToolInstaller
import java.io.File

/**
 * High-level install/remove/update API used by the UI layer. Wraps
 * ToolInstaller + VersionManager + ToolRegistry.
 */
class ToolManager(private val context: Context, private val registry: ToolRegistry) {

    private val prefixDir = File(context.filesDir, "usr")
    private val installer = ToolInstaller(context, prefixDir)
    private val versionManager = VersionManager(context)

    fun install(toolName: String, onProgress: (Float) -> Unit = {}): Result<Unit> {
        val tool = registry.findByName(toolName)
            ?: return Result.failure(IllegalArgumentException("Unknown tool: $toolName"))

        tool.depends.forEach { dep ->
            if (!versionManager.isInstalled(dep)) install(dep)
        }

        return installer.install(tool, onProgress).onSuccess {
            versionManager.markInstalled(tool.name, tool.version)
        }
    }

    fun remove(toolName: String): Result<Unit> {
        val tool = registry.findByName(toolName)
            ?: return Result.failure(IllegalArgumentException("Unknown tool: $toolName"))
        return installer.remove(tool).onSuccess {
            versionManager.markRemoved(tool.name)
        }
    }

    fun isInstalled(toolName: String) = versionManager.isInstalled(toolName)

    fun installedTools() = versionManager.allInstalled()
}
