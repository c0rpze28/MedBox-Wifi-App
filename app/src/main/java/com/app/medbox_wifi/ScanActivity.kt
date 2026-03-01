package com.app.medbox_wifi

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var ivCapturedImage: ImageView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var bottomSheet: CardView
    private lateinit var fabCapture: FloatingActionButton
    private lateinit var tvMedicineName: TextView
    private lateinit var tvGenericName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnSave: Button
    private lateinit var btnClose: Button
    
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private val ocrEngine: OcrEngine = MlKitOcrEngine()
    private lateinit var database: AppDatabase
    private var isProcessingCapture = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        viewFinder = findViewById(R.id.viewFinder)
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        graphicOverlay = findViewById(R.id.graphicOverlay)
        bottomSheet = findViewById(R.id.bottomSheet)
        fabCapture = findViewById(R.id.fabCapture)
        tvMedicineName = findViewById(R.id.tvMedicineName)
        tvGenericName = findViewById(R.id.tvGenericName)
        tvDescription = findViewById(R.id.tvDescription)
        btnSave = findViewById(R.id.btnSave)
        btnClose = findViewById(R.id.btnClose)
        
        database = AppDatabase.getDatabase(this, lifecycleScope)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        fabCapture.setOnClickListener {
            takePhoto()
        }

        btnSave.setOnClickListener {
            val text = tvMedicineName.text.toString()
            if (text.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    database.scannedTextDao().insert(ScannedText(content = text))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ScanActivity, "Logged: $text", Toast.LENGTH_SHORT).show()
                        resetScanner()
                    }
                }
            }
        }

        btnClose.setOnClickListener {
            resetScanner()
        }
    }

    private fun resetScanner() {
        isProcessingCapture = false
        ivCapturedImage.visibility = View.GONE
        bottomSheet.visibility = View.GONE
        fabCapture.visibility = View.VISIBLE
        graphicOverlay.clear()
        graphicOverlay.stopAnimation()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        isProcessingCapture = true
        fabCapture.visibility = View.GONE
        graphicOverlay.startAnimation()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @OptIn(ExperimentalGetImage::class)
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    ivCapturedImage.setImageBitmap(bitmap)
                    ivCapturedImage.visibility = View.VISIBLE

                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val width = imageProxy.width
                        val height = imageProxy.height
                        
                        ocrEngine.processImage(mediaImage, rotationDegrees) { blocks ->
                            processOcrResults(blocks, width, height, rotationDegrees)
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(baseContext, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    resetScanner()
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isProcessingCapture) {
                            processLiveAnalysis(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera bind failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processLiveAnalysis(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val width = imageProxy.width
            val height = imageProxy.height
            
            ocrEngine.processImage(mediaImage, rotationDegrees) { blocks ->
                lifecycleScope.launch(Dispatchers.Default) {
                    val graphics = blocks.filter { it.text.length > 3 && it.text.any { c -> c.isLetter() } }
                        .map { block ->
                            val cleanText = block.text.replace("[^A-Za-z0-9 ]".toRegex(), " ")
                            val match = database.medicineDao().findMatchingMedicine(cleanText)
                            GraphicOverlay.TextGraphic(graphicOverlay, block, match != null)
                        }

                    withContext(Dispatchers.Main) {
                        graphicOverlay.clear()
                        graphicOverlay.setTransformationInfo(width, height, rotationDegrees)
                        for (g in graphics) graphicOverlay.add(g)
                    }
                }
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    private fun processOcrResults(blocks: List<TextBlock>, width: Int, height: Int, rotationDegrees: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            var bestMatch: Medicine? = null
            val graphics = mutableListOf<GraphicOverlay.TextGraphic>()
            
            // Priority 1: Look for any Brand Name match across all blocks
            for (block in blocks) {
                val cleanBlockText = block.text.replace("[^A-Za-z0-9 ]".toRegex(), " ").trim()
                val match = database.medicineDao().findMatchingMedicine(cleanBlockText)
                
                if (match != null) {
                    // If we found a brand match in the text, prioritize it
                    if (cleanBlockText.uppercase().contains(match.brandName.uppercase())) {
                        bestMatch = match
                        // Don't break, continue to mark all matches visually
                    } else if (bestMatch == null) {
                        // generic match, keep it if we haven't found a brand match yet
                        bestMatch = match
                    }
                }
                
                if (block.text.length > 3 && block.text.any { it.isLetter() }) {
                    graphics.add(GraphicOverlay.TextGraphic(graphicOverlay, block, match != null))
                }
            }

            withContext(Dispatchers.Main) {
                graphicOverlay.clear()
                graphicOverlay.setTransformationInfo(width, height, rotationDegrees)
                for (g in graphics) graphicOverlay.add(g)

                if (bestMatch != null) {
                    tvMedicineName.text = bestMatch.brandName
                    tvGenericName.text = bestMatch.genericName
                    tvDescription.text = bestMatch.description
                    bottomSheet.visibility = View.VISIBLE
                    graphicOverlay.stopAnimation()
                } else {
                    Toast.makeText(this@ScanActivity, "No medicine detected. Try angling the camera.", Toast.LENGTH_LONG).show()
                    resetScanner()
                }
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
