package com.aashutarminal.terminal

/**
 * Owns one PTY-backed shell process. Bridges to native code
 * (see app/src/main/cpp/pty_manager.cpp) via JNI.
 */
class TerminalSession(
    private val shellPath: String,
    private val args: Array<String>,
    private val cwd: String,
    private val envVars: Array<String>
) {
    private var pid: Int = -1
    private var ptyFd: Int = -1

    val emulator = TerminalEmulator(cols = 80, rows = 24)

    private var readerThread: Thread? = null
    @Volatile private var running = false

    fun start() {
        val result = TerminalNative.createSubprocess(shellPath, cwd, args, envVars)
        ptyFd = result[0]
        pid = result[1]
        running = true

        readerThread = Thread {
            val buffer = ByteArray(4096)
            while (running) {
                val n = TerminalNative.read(ptyFd, buffer)
                if (n <= 0) break
                emulator.feed(buffer, n)
            }
        }.apply { start() }
    }

    fun write(data: ByteArray) {
        if (ptyFd >= 0) TerminalNative.write(ptyFd, data)
    }

    fun resize(cols: Int, rows: Int) {
        emulator.resize(cols, rows)
        if (ptyFd >= 0) TerminalNative.resize(ptyFd, cols, rows)
    }

    fun close() {
        running = false
        if (pid > 0) TerminalNative.killProcess(pid)
        if (ptyFd >= 0) TerminalNative.closeFd(ptyFd)
        readerThread?.interrupt()
    }
}

/** Thin JNI wrapper — implemented in native-lib.h / pty_manager.cpp. */
internal object TerminalNative {
    init {
        System.loadLibrary("aashuterminal")
    }

    external fun createSubprocess(
        shellPath: String,
        cwd: String,
        args: Array<String>,
        envVars: Array<String>
    ): IntArray // [ptyFd, pid]

    external fun read(fd: Int, buffer: ByteArray): Int
    external fun write(fd: Int, data: ByteArray): Int
    external fun resize(fd: Int, cols: Int, rows: Int)
    external fun closeFd(fd: Int)
    external fun killProcess(pid: Int)
}
