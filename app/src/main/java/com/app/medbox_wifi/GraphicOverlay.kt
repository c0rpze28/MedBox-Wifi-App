package com.app.medbox_wifi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.View

class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val lock = Any()
    private val graphics = mutableListOf<Graphic>()
    private val transformationMatrix = Matrix()

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)
        
        fun calculateRect(left: Float, top: Float, right: Float, bottom: Float) = 
            overlay.transformationMatrix.mapRect(android.graphics.RectF(left, top, right, bottom))
            
        fun postInvalidate() = overlay.postInvalidate()
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

    fun setTransformationInfo(imageWidth: Int, imageHeight: Int) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        transformationMatrix.reset()
        transformationMatrix.setScale(scaleX, scaleY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}
