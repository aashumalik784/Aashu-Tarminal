package com.aashutarminal

import android.app.Application
import com.aashutarminal.bootstrap.BootstrapManager
import com.aashutarminal.tools.ToolRegistry

/**
 * Application entry point. Initializes singletons that need a Context
 * (bootstrap manager, tool registry) once, at process start.
 */
class AashuApp : Application() {

    lateinit var bootstrapManager: BootstrapManager
        private set

    lateinit var toolRegistry: ToolRegistry
        private set

    override fun onCreate() {
        super.onCreate()
        bootstrapManager = BootstrapManager(this)
        toolRegistry = ToolRegistry(this)
        toolRegistry.loadAsync()
    }

    companion object {
        lateinit var instance: AashuApp
            private set
    }

    init {
        instance = this
    }
}
