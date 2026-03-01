package com.app.medbox_wifi

import android.media.Image
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun processImage(image: Image, rotationDegrees: Int, onResult: (List<TextBlock>) -> Unit) {
        val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { block ->
                    TextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox,
                        lines = block.lines.map { line ->
                            TextLine(line.text, line.boundingBox)
                        }
                    )
                }
                onResult(blocks)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}
