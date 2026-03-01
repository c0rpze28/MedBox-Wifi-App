package com.app.medbox_wifi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val lock = Any()
    private val graphics = mutableListOf<Graphic>()
    private val transformationMatrix = Matrix()
    
    // Lens-style colors
    private val dotColor = Color.parseColor("#FFFFFF")
    private val highlightColor = Color.parseColor("#4285F4") // Google Blue

    private val dotPaint = Paint().apply {
        color = dotColor
        style = Paint.Style.FILL
        alpha = 180
        isAntiAlias = true
    }

    private val highlightPaint = Paint().apply {
        color = highlightColor
        style = Paint.Style.FILL
        alpha = 60
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    abstract class Graphic(protected val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)
        
        fun translateRect(rect: RectF): RectF {
            val translatedRect = RectF(rect)
            overlay.transformationMatrix.mapRect(translatedRect)
            return translatedRect
        }
    }

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
        postInvalidate()
    }

    /**
     * Correctly maps the coordinates from ML Kit (rotated image space) to the View.
     */
    fun setTransformationInfo(imageWidth: Int, imageHeight: Int, rotationDegrees: Int) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth == 0f || viewHeight == 0f) return

        val matrix = Matrix()

        // ML Kit accounts for rotation internally, so the bounding box is relative 
        // to the rotated dimensions.
        val rotatedWidth = if (rotationDegrees % 180 == 90) imageHeight.toFloat() else imageWidth.toFloat()
        val rotatedHeight = if (rotationDegrees % 180 == 90) imageWidth.toFloat() else imageHeight.toFloat()

        // Scale to fit "CenterCrop" behavior
        val scale = max(viewWidth / rotatedWidth, viewHeight / rotatedHeight)
        matrix.postScale(scale, scale)

        // Center the coordinate system
        val offsetX = (viewWidth - rotatedWidth * scale) / 2f
        val offsetY = (viewHeight - rotatedHeight * scale) / 2f
        matrix.postTranslate(offsetX, offsetY)

        synchronized(lock) {
            transformationMatrix.set(matrix)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            for (graphic in graphics) {
                if (graphic is TextGraphic) {
                    val rect = graphic.getTranslatedRect()
                    
                    if (graphic.isMatch) {
                        // Highlight matched medicine with a subtle glow box
                        canvas.drawRoundRect(rect, 8f, 8f, highlightPaint)
                        canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
                    } else {
                        // Draw Lens-style "searching" dots for other text
                        canvas.drawCircle(rect.centerX(), rect.centerY(), 8f, dotPaint)
                    }
                }
            }
        }
    }

    class TextGraphic(
        overlay: GraphicOverlay, 
        private val block: TextBlock,
        val isMatch: Boolean = false
    ) : Graphic(overlay) {
        private var cachedRect: RectF? = null

        fun getTranslatedRect(): RectF {
            return cachedRect ?: translateRect(RectF(block.boundingBox)).also { cachedRect = it }
        }

        override fun draw(canvas: Canvas) {
            // Drawing is handled in batch in onDraw for performance
        }
    }
}
