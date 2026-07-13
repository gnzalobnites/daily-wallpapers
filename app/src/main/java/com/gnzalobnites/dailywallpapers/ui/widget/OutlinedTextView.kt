package com.gnzalobnites.dailywallpapers.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.gnzalobnites.dailywallpapers.R

/**
 * TextView que dibuja un borde (stroke) alrededor de su texto para mantenerlo
 * legible sobre cualquier imagen de fondo, sin depender de un scrim/degradado
 * oscuro superpuesto a la imagen.
 *
 * Uso en XML:
 *   <com.gnzalobnites.dailywallpapers.ui.widget.OutlinedTextView
 *       app:outlineColor="#99000000"
 *       app:outlineWidth="3dp" ... />
 */
class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var outlineColor: Int = Color.parseColor("#99000000")
    private var outlineWidthPx: Float = 3f * resources.displayMetrics.density
    private var fillColor: Int = currentTextColor

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.OutlinedTextView)
            outlineColor = a.getColor(R.styleable.OutlinedTextView_outlineColor, outlineColor)
            outlineWidthPx = a.getDimension(R.styleable.OutlinedTextView_outlineWidth, outlineWidthPx)
            a.recycle()
        }
        fillColor = currentTextColor
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        fillColor = color
    }

    override fun onDraw(canvas: Canvas) {
        // Primera pasada: borde (stroke)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outlineWidthPx
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeMiter = 2f
        super.setTextColor(outlineColor)
        super.onDraw(canvas)

        // Segunda pasada: relleno
        paint.style = Paint.Style.FILL
        super.setTextColor(fillColor)
        super.onDraw(canvas)
    }
}
