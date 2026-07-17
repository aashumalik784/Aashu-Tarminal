package com.aashutarminal.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Renders a TerminalEmulator's screen buffer (colors + bold included) and
 * forwards keyboard/soft-keyboard input into the session, similar to how
 * Termux's TerminalView works.
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var session: TerminalSession? = null
        set(value) {
            field = value
            value?.emulator?.addChangeListener { postInvalidate() }
        }

    private val textPaint = Paint().apply {
        textSize = 32f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val bgPaint = Paint()
    private val cursorPaint = Paint().apply { color = Color.argb(160, 0, 229, 160) }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = EditorInfo.TYPE_NULL
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        return object : BaseInputConnection(this, true) {
            override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
                session?.write(text.toString().toByteArray(Charsets.UTF_8))
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_ENTER -> session?.write(byteArrayOf('\r'.code.toByte()))
                        KeyEvent.KEYCODE_DEL -> session?.write(byteArrayOf(127.toByte()))
                        else -> return super.sendKeyEvent(event)
                    }
                    return true
                }
                return super.sendKeyEvent(event)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                repeat(beforeLength) { session?.write(byteArrayOf(127.toByte())) }
                return true
            }
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(this, 0)
        }
        return true
    }

    /** Sends a raw control sequence (used by the extra-keys row: ESC, TAB, CTRL, arrows...). */
    fun sendRaw(bytes: ByteArray) = session?.write(bytes)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val emu = session?.emulator ?: return
        val cellW = textPaint.measureText("M")
        val cellH = textPaint.textSize * 1.2f

        val screen = emu.snapshot()
        for (row in screen.indices) {
            for (col in screen[row].indices) {
                val cell = screen[row][col]
                bgPaint.color = 0xFF000000.toInt() or cell.bg
                canvas.drawRect(
                    col * cellW, row * cellH,
                    (col + 1) * cellW, (row + 1) * cellH,
                    bgPaint
                )
                if (cell.char != ' ') {
                    textPaint.color = 0xFF000000.toInt() or cell.fg
                    textPaint.isFakeBoldText = cell.bold
                    canvas.drawText(cell.char.toString(), col * cellW, (row + 1) * cellH - 6f, textPaint)
                }
            }
        }

        // cursor
        canvas.drawRect(
            emu.cursorCol * cellW, emu.cursorRow * cellH,
            (emu.cursorCol + 1) * cellW, (emu.cursorRow + 1) * cellH,
            cursorPaint
        )
    }
}
