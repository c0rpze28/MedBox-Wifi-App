package com.app.medbox_wifi

import android.graphics.Rect
import android.media.Image

data class TextBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<TextLine>
)

data class TextLine(
    val text: String,
    val boundingBox: Rect?
)

interface OcrEngine {
    fun processImage(
        image: Image, 
        rotationDegrees: Int, 
        onResult: (List<TextBlock>) -> Unit
    )
}
