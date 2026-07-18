package com.aashutarminal

import android.app.Application
import com.aashutarminal.bootstrap.BootstrapManager
import com.aashutarminal.tools.ToolRegistry
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

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
        installCrashLogger()

        bootstrapManager = BootstrapManager(this)
        toolRegistry = ToolRegistry(this)

        // Any exception here must not crash the whole app before the UI
        // even shows -- tool registry loading is best-effort at startup.
        runCatching { toolRegistry.loadAsync() }
    }

    /**
     * Writes any uncaught exception to a plain-text file under this app's
     * own external-storage folder (no storage permission needed on any
     * Android version) so a crash can be diagnosed without adb/logcat.
     * Path: Android/data/com.aashutarminal[.debug]/files/aashu-tarminal-crash.txt
     */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val dir = getExternalFilesDir(null) ?: filesDir
                val outFile = File(dir, "aashu-tarminal-crash.txt")
                outFile.writeText(
                    "Aashu Tarminal crash log\n" +
                    "Thread: ${thread.name}\n\n" +
                    sw.toString()
                )
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        lateinit var instance: AashuApp
            private set
    }

    init {
        instance = this
    }
}
