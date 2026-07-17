package com.aashutarminal.tools

object ToolCategories {
    const val LANGUAGES = "languages"
    const val DATABASES = "databases"
    const val DEVOPS = "devops"
    const val NETWORKING = "networking"
    const val EDITORS = "editors"
    const val BUILD_TOOLS = "build-tools"
    const val PACKAGE_MANAGERS = "package-managers"
    const val CLOUD_TOOLS = "cloud-tools"
    const val SECURITY = "security"
    const val MISC = "misc"

    val ALL = listOf(
        LANGUAGES, DATABASES, DEVOPS, NETWORKING, EDITORS,
        BUILD_TOOLS, PACKAGE_MANAGERS, CLOUD_TOOLS, SECURITY, MISC
    )
}

data class Tool(
    val name: String,
    val category: String,
    val version: String,
    val description: String,
    val binary: String,
    val sizeMb: Int,
    val downloadUrl: String = "",
    val sha256: String = "",
    val depends: List<String> = emptyList()
)
