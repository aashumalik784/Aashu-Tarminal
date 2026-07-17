package com.aashutarminal.terminal

/**
 * Minimal VT100/xterm-subset emulator: maintains a screen buffer of
 * (char, fgColor, bgColor, attrs) cells and updates it as bytes are fed
 * in. A full escape-sequence parser (CSI/OSC/DCS) should be built out
 * here; this is the structural skeleton.
 */
class TerminalEmulator(private var cols: Int, private var rows: Int) {

    data class Cell(var char: Char = ' ', var fg: Int = 0xFFFFFF, var bg: Int = 0x000000)

    private var screen: Array<Array<Cell>> = buildScreen(cols, rows)
    var cursorRow = 0
        private set
    var cursorCol = 0
        private set

    private val listeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) = listeners.add(listener)

    fun resize(newCols: Int, newRows: Int) {
        cols = newCols
        rows = newRows
        screen = buildScreen(cols, rows)
        cursorRow = 0
        cursorCol = 0
        notifyChange()
    }

    fun feed(data: ByteArray, length: Int) {
        // TODO: parse ANSI escape sequences (CSI cursor moves, SGR colors,
        // OSC titles, etc). For now: printable bytes advance the cursor,
        // '\n' and '\r' behave as expected. Escape sequences are skipped.
        var i = 0
        while (i < length) {
            val b = data[i].toInt().toChar()
            when (b) {
                '\n' -> { cursorRow = (cursorRow + 1).coerceAtMost(rows - 1); cursorCol = 0 }
                '\r' -> cursorCol = 0
                '\u001B' -> i += skipEscapeSequence(data, i, length)
                else -> {
                    if (cursorRow < rows && cursorCol < cols) {
                        screen[cursorRow][cursorCol].char = b
                    }
                    cursorCol++
                    if (cursorCol >= cols) { cursorCol = 0; cursorRow = (cursorRow + 1).coerceAtMost(rows - 1) }
                }
            }
            i++
        }
        notifyChange()
    }

    private fun skipEscapeSequence(data: ByteArray, start: Int, length: Int): Int {
        // Very rough CSI skipper: ESC [ ... <final byte 0x40-0x7E>
        var i = start + 1
        if (i < length && data[i].toInt().toChar() == '[') {
            i++
            while (i < length) {
                val c = data[i].toInt().toChar()
                if (c in '\u0040'..'\u007E') { i++; break }
                i++
            }
        }
        return i - start
    }

    fun snapshot(): Array<Array<Cell>> = screen

    private fun notifyChange() = listeners.forEach { it() }

    private fun buildScreen(c: Int, r: Int) = Array(r) { Array(c) { Cell() } }
}
