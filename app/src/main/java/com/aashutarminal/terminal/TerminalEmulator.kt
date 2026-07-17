package com.aashutarminal.terminal

/**
 * VT100/xterm-subset emulator: maintains a screen buffer of
 * (char, fgColor, bgColor, attrs) cells and updates it as bytes are fed
 * in. Supports the escape sequences a normal shell session needs day to
 * day: cursor movement, screen/line erase, and SGR color/attribute codes.
 * OSC/DCS sequences are consumed (so they don't leak into the visible
 * buffer) but not acted upon.
 */
class TerminalEmulator(private var cols: Int, private var rows: Int) {

    data class Cell(
        var char: Char = ' ',
        var fg: Int = DEFAULT_FG,
        var bg: Int = DEFAULT_BG,
        var bold: Boolean = false
    )

    companion object {
        const val DEFAULT_FG = 0xE6EDF3
        const val DEFAULT_BG = 0x0D1117

        // Standard 8-color ANSI palette (30-37 / 40-47), bright via bold.
        val ANSI_COLORS = intArrayOf(
            0x000000, 0xCC4444, 0x44CC44, 0xCCCC44,
            0x4477CC, 0xCC44CC, 0x44CCCC, 0xCCCCCC
        )
        val ANSI_BRIGHT_COLORS = intArrayOf(
            0x666666, 0xFF6666, 0x66FF66, 0xFFFF66,
            0x6699FF, 0xFF66FF, 0x66FFFF, 0xFFFFFF
        )
    }

    private var screen: Array<Array<Cell>> = buildScreen(cols, rows)
    var cursorRow = 0
        private set
    var cursorCol = 0
        private set

    private var curFg = DEFAULT_FG
    private var curBg = DEFAULT_BG
    private var curBold = false

    private val listeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) = listeners.add(listener)

    fun resize(newCols: Int, newRows: Int) {
        val old = screen
        cols = newCols
        rows = newRows
        screen = buildScreen(cols, rows)
        for (r in old.indices) {
            if (r >= rows) break
            for (c in old[r].indices) {
                if (c >= cols) break
                screen[r][c] = old[r][c]
            }
        }
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
        notifyChange()
    }

    fun feed(data: ByteArray, length: Int) {
        var i = 0
        while (i < length) {
            val b = data[i].toInt().toChar()
            when (b) {
                '\n' -> { newLine() }
                '\r' -> cursorCol = 0
                '\b' -> if (cursorCol > 0) cursorCol--
                '\u001B' -> i += handleEscape(data, i, length) - 1
                else -> putChar(b)
            }
            i++
        }
        notifyChange()
    }

    private fun putChar(c: Char) {
        if (cursorRow in 0 until rows && cursorCol in 0 until cols) {
            screen[cursorRow][cursorCol] = Cell(c, curFg, curBg, curBold)
        }
        cursorCol++
        if (cursorCol >= cols) { cursorCol = 0; newLine() }
    }

    private fun newLine() {
        if (cursorRow < rows - 1) {
            cursorRow++
        } else {
            // scroll up by one line
            for (r in 0 until rows - 1) screen[r] = screen[r + 1]
            screen[rows - 1] = Array(cols) { Cell(fg = curFg, bg = curBg) }
        }
    }

    /** Returns number of bytes consumed starting at the ESC byte. */
    private fun handleEscape(data: ByteArray, start: Int, length: Int): Int {
        if (start + 1 >= length) return 1
        val next = data[start + 1].toInt().toChar()

        return when (next) {
            '[' -> handleCsi(data, start, length)
            ']' -> handleOsc(data, start, length) // OSC ... BEL or ST
            else -> 2 // unknown 2-byte escape, skip it
        }
    }

    private fun handleCsi(data: ByteArray, start: Int, length: Int): Int {
        var i = start + 2 // skip ESC [
        val paramStart = i
        while (i < length && data[i].toInt().toChar() !in '\u0040'..'\u007E') i++
        if (i >= length) return i - start
        val finalByte = data[i].toInt().toChar()
        val paramStr = String(CharArray(i - paramStart) { data[paramStart + it].toInt().toChar() })
        val params = paramStr.split(';').mapNotNull { it.toIntOrNull() }

        when (finalByte) {
            'A' -> cursorRow = (cursorRow - (params.getOrElse(0) { 1 })).coerceAtLeast(0)
            'B' -> cursorRow = (cursorRow + (params.getOrElse(0) { 1 })).coerceAtMost(rows - 1)
            'C' -> cursorCol = (cursorCol + (params.getOrElse(0) { 1 })).coerceAtMost(cols - 1)
            'D' -> cursorCol = (cursorCol - (params.getOrElse(0) { 1 })).coerceAtLeast(0)
            'H', 'f' -> {
                cursorRow = ((params.getOrElse(0) { 1 }) - 1).coerceIn(0, rows - 1)
                cursorCol = ((params.getOrElse(1) { 1 }) - 1).coerceIn(0, cols - 1)
            }
            'J' -> eraseScreen(params.getOrElse(0) { 0 })
            'K' -> eraseLine(params.getOrElse(0) { 0 })
            'm' -> applySgr(if (params.isEmpty()) listOf(0) else params)
            else -> { /* unsupported CSI final byte, ignore */ }
        }
        return (i - start) + 1
    }

    private fun handleOsc(data: ByteArray, start: Int, length: Int): Int {
        // OSC sequences end with BEL (0x07) or ESC \\ (ST)
        var i = start + 2
        while (i < length) {
            val c = data[i].toInt().toChar()
            if (c == '\u0007') { i++; break }
            if (c == '\u001B' && i + 1 < length && data[i + 1].toInt().toChar() == '\\') { i += 2; break }
            i++
        }
        return i - start
    }

    private fun eraseScreen(mode: Int) {
        when (mode) {
            0 -> for (r in cursorRow until rows) for (c in (if (r == cursorRow) cursorCol else 0) until cols) screen[r][c] = Cell(fg = curFg, bg = curBg)
            1 -> for (r in 0..cursorRow) for (c in 0 until (if (r == cursorRow) cursorCol + 1 else cols)) screen[r][c] = Cell(fg = curFg, bg = curBg)
            2, 3 -> for (r in 0 until rows) for (c in 0 until cols) screen[r][c] = Cell(fg = curFg, bg = curBg)
        }
    }

    private fun eraseLine(mode: Int) {
        when (mode) {
            0 -> for (c in cursorCol until cols) screen[cursorRow][c] = Cell(fg = curFg, bg = curBg)
            1 -> for (c in 0..cursorCol) screen[cursorRow][c] = Cell(fg = curFg, bg = curBg)
            2 -> for (c in 0 until cols) screen[cursorRow][c] = Cell(fg = curFg, bg = curBg)
        }
    }

    private fun applySgr(params: List<Int>) {
        var idx = 0
        while (idx < params.size) {
            when (val p = params[idx]) {
                0 -> { curFg = DEFAULT_FG; curBg = DEFAULT_BG; curBold = false }
                1 -> curBold = true
                22 -> curBold = false
                39 -> curFg = DEFAULT_FG
                49 -> curBg = DEFAULT_BG
                in 30..37 -> curFg = if (curBold) ANSI_BRIGHT_COLORS[p - 30] else ANSI_COLORS[p - 30]
                in 40..47 -> curBg = ANSI_COLORS[p - 40]
                in 90..97 -> curFg = ANSI_BRIGHT_COLORS[p - 90]
                in 100..107 -> curBg = ANSI_BRIGHT_COLORS[p - 100]
            }
            idx++
        }
    }

    fun snapshot(): Array<Array<Cell>> = screen

    private fun notifyChange() = listeners.forEach { it() }

    private fun buildScreen(c: Int, r: Int) = Array(r) { Array(c) { Cell() } }
}
