package com.aashutarminal.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Renders a TerminalEmulator's screen buffer. The heavy-lifting glyph
 * rasterization/caching happens natively (see renderer.cpp / glyph_atlas.cpp);
 * this View is the Android-side surface that blits the result and forwards
 * input events into the session.
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
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val bgPaint = Paint().apply { color = Color.BLACK }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val emu = session?.emulator ?: return
        val cellW = textPaint.measureText("M")
        val cellH = textPaint.textSize * 1.2f

        val screen = emu.snapshot()
        for (row in screen.indices) {
            for (col in screen[row].indices) {
                val cell = screen[row][col]
                if (cell.char != ' ') {
                    canvas.drawText(
                        cell.char.toString(),
                        col * cellW,
                        (row + 1) * cellH,
                        textPaint
                    )
                }
            }
        }
    }
}
