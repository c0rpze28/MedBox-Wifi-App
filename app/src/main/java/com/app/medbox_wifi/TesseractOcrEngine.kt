package com.app.medbox_wifi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class TesseractOcrEngine(private val context: Context) : OcrEngine {
    private val tessBaseAPI = TessBaseAPI()
    private val datapath = "${context.filesDir}/tesseract/"
    private val language = "eng"

    init {
        prepareTesseract()
        tessBaseAPI.init(datapath, language)
    }

    private fun prepareTesseract() {
        val dir = File(datapath, "tessdata")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val assetFile = "eng.traineddata"
        val outFile = File(dir, assetFile)
        if (!outFile.exists()) {
            context.assets.open("tessdata/$assetFile").use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    override fun processImage(image: Image, rotationDegrees: Int, onResult: (List<TextBlock>) -> Unit) {
        val bitmap = imageToBitmap(image)
        if (bitmap != null) {
            tessBaseAPI.setImage(bitmap)
            val text = tessBaseAPI.utF8Text
            val result = if (text.isNullOrBlank()) {
                emptyList()
            } else {
                listOf(TextBlock(text, null, emptyList()))
            }
            onResult(result)
        } else {
            onResult(emptyList())
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
