package com.app.medbox_wifi

import android.media.Image

interface OcrEngine {
    fun processImage(image: Image, rotationDegrees: Int, onResult: (String) -> Unit)
}
