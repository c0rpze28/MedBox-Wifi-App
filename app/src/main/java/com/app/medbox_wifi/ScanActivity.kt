package com.app.medbox_wifi

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var bottomSheet: LinearLayout
    private lateinit var tvDetectedText: TextView
    private lateinit var btnSave: Button
    
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var ocrEngine: OcrEngine
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        viewFinder = findViewById(R.id.viewFinder)
        graphicOverlay = findViewById(R.id.graphicOverlay)
        bottomSheet = findViewById(R.id.bottomSheet)
        tvDetectedText = findViewById(R.id.tvDetectedText)
        btnSave = findViewById(R.id.btnSave)
        
        database = AppDatabase.getDatabase(this)
        ocrEngine = MlKitOcrEngine() // Switched back to ML Kit

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        btnSave.setOnClickListener {
            val text = tvDetectedText.text.toString()
            if (text.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    database.scannedTextDao().insert(ScannedText(content = text))
                    runOnUiThread {
                        Toast.makeText(this@ScanActivity, "Saved!", Toast.LENGTH_SHORT).show()
                        bottomSheet.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera bind failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            ocrEngine.processImage(mediaImage, imageProxy.imageInfo.rotationDegrees) { blocks ->
                runOnUiThread {
                    graphicOverlay.clear()
                    graphicOverlay.setTransformationInfo(imageProxy.width, imageProxy.height)
                    
                    if (blocks.isNotEmpty()) {
                        // Show the first block text in the bottom sheet
                        tvDetectedText.text = blocks[0].text
                        bottomSheet.visibility = View.VISIBLE
                    } else {
                        bottomSheet.visibility = View.GONE
                    }
                    
                    for (block in blocks) {
                        val graphic = TextGraphic(graphicOverlay, block)
                        graphicOverlay.add(graphic)
                    }
                }
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    private class TextGraphic(
        overlay: GraphicOverlay,
        private val block: TextBlock
    ) : GraphicOverlay.Graphic(overlay) {
        private val rectPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        override fun draw(canvas: android.graphics.Canvas) {
            block.boundingBox?.let { rect ->
                val rectF = RectF(rect)
                canvas.drawRect(rectF, rectPaint)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
