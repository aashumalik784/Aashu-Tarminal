package com.aashutarminal.tools

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Loads the merged tools.json (generated from tools-registry/tools/*.json
 * by tools-registry/generate_registry.py) shipped in assets.
 */
class ToolRegistry(private val context: Context) {

    @Serializable
    private data class ToolJson(
        val name: String,
        val category: String,
        val version: String,
        val description: String,
        val binary: String,
        val size_mb: Int,
        val download_url: String = "",
        val sha256: String = "",
        val depends: List<String> = emptyList()
    )

    private var tools: List<Tool> = emptyList()

    fun loadAsync() {
        CoroutineScope(Dispatchers.IO).launch { tools = loadAll() }
    }

    fun loadAll(): List<Tool> {
        if (tools.isNotEmpty()) return tools
        val text = context.assets.open("tools/tools.json").bufferedReader().readText()
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<List<ToolJson>>(text)
        tools = parsed.map {
            Tool(it.name, it.category, it.version, it.description, it.binary, it.size_mb, it.download_url, it.sha256, it.depends)
        }
        return tools
    }

    fun findByName(name: String): Tool? = loadAll().firstOrNull { it.name == name }

    fun byCategory(category: String): List<Tool> = loadAll().filter { it.category == category }

    fun search(query: String): List<Tool> =
        loadAll().filter { it.name.contains(query, true) || it.description.contains(query, true) }
}
