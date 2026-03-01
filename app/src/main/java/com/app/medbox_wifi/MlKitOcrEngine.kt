package com.app.medbox_wifi

import android.media.Image
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun processImage(image: Image, rotationDegrees: Int, onResult: (String) -> Unit) {
        val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text)
            }
            .addOnFailureListener { e ->
                onResult("Error: ${e.message}")
            }
    }
}
