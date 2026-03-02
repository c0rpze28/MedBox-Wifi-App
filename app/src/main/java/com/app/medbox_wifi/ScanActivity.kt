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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var ivCapturedImage: ImageView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var bottomSheet: CardView
    private lateinit var tvMedicineName: TextView
    private lateinit var tvGenericName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnSave: Button
    private lateinit var btnClose: Button
    private lateinit var fabCapture: FloatingActionButton
    
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private val ocrEngine: OcrEngine = MlKitOcrEngine()
    private lateinit var database: AppDatabase
    private var isProcessingCapture = false
    
    private var detectedExpiryDate: Long = 0
    private var detectedDosage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        viewFinder = findViewById(R.id.viewFinder)
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        graphicOverlay = findViewById(R.id.graphicOverlay)
        bottomSheet = findViewById(R.id.bottomSheet)
        tvMedicineName = findViewById(R.id.tvMedicineName)
        tvGenericName = findViewById(R.id.tvGenericName)
        tvDescription = findViewById(R.id.tvDescription)
        btnSave = findViewById(R.id.btnSave)
        btnClose = findViewById(R.id.btnClose)
        fabCapture = findViewById(R.id.fabCapture)
        
        database = AppDatabase.getDatabase(this, lifecycleScope)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        fabCapture.setOnClickListener { takePhoto() }
        btnClose.setOnClickListener { resetScanner() }
        btnSave.setOnClickListener { checkAndSave() }
    }

    private fun checkAndSave() {
        val brand = tvMedicineName.text.toString()
        val generic = tvGenericName.text.toString()
        
        lifecycleScope.launch(Dispatchers.Main) {
            val existing = withContext(Dispatchers.IO) {
                database.loggedMedicineDao().getByBrandName(brand)
            }
            
            if (existing != null) {
                Toast.makeText(this@ScanActivity, "$brand is already logged.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val currentLogs = withContext(Dispatchers.IO) {
                database.loggedMedicineDao().getRecentLogs()
            }
            
            if (currentLogs.size >= 6) {
                showReplacementDialog(brand, generic, currentLogs)
            } else {
                saveMedicine(brand, generic)
            }
        }
    }

    private fun showReplacementDialog(newBrand: String, newGeneric: String, currentLogs: List<LoggedMedicine>) {
        val medicineNames = currentLogs.map { it.brandName }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Replace an entry?")
            .setItems(medicineNames) { _, which ->
                val toReplace = currentLogs[which]
                lifecycleScope.launch(Dispatchers.IO) {
                    database.loggedMedicineDao().delete(toReplace)
                    withContext(Dispatchers.Main) {
                        saveMedicine(newBrand, newGeneric)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMedicine(brand: String, generic: String) {
        val newLog = LoggedMedicine(
            brandName = brand, 
            genericName = generic,
            expiryDate = detectedExpiryDate,
            dosage = detectedDosage
        )
        lifecycleScope.launch(Dispatchers.IO) {
            database.loggedMedicineDao().insert(newLog)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ScanActivity, "Logged: $brand", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun resetScanner() {
        isProcessingCapture = false
        ivCapturedImage.visibility = View.GONE
        bottomSheet.visibility = View.GONE
        fabCapture.visibility = View.VISIBLE
        graphicOverlay.clear()
        graphicOverlay.stopAnimation()
        detectedExpiryDate = 0
        detectedDosage = ""
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
                        ocrEngine.processImage(mediaImage, imageProxy.imageInfo.rotationDegrees) { blocks ->
                            processOcrResults(blocks, imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees)
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(baseContext, "Capture failed", Toast.LENGTH_SHORT).show()
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
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isProcessingCapture) processLiveAnalysis(imageProxy) else imageProxy.close()
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera bind failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processLiveAnalysis(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            ocrEngine.processImage(mediaImage, imageProxy.imageInfo.rotationDegrees) { blocks ->
                lifecycleScope.launch(Dispatchers.Default) {
                    val graphics = blocks.filter { it.text.length > 3 && it.text.any { c -> c.isLetter() } }
                        .map { block ->
                            val cleanText = block.text.replace("[^A-Za-z0-9 ]".toRegex(), " ")
                            val isMatch = database.medicineDao().findMatchingMedicine(cleanText) != null
                            GraphicOverlay.TextGraphic(graphicOverlay, block, isMatch)
                        }
                    withContext(Dispatchers.Main) {
                        graphicOverlay.clear()
                        graphicOverlay.setTransformationInfo(imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees)
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
            val fullRawText = blocks.joinToString("\n") { it.text }
            
            // Comprehensive cleaning for better identification
            val cleanedTextForSearch = fullRawText.uppercase()
                .replace("(?<=\\d)O|O(?=\\d)".toRegex(), "0") // OCR O->0 fix
                .replace("[^A-Z0-9 ]".toRegex(), " ")
                .replace("\\s+".toRegex(), " ")

            val medicines = database.medicineDao().getAllMedicines()
            var bestMatch: Medicine? = null
            var highestScore = -1.0
            
            for (med in medicines) {
                val brand = med.brandName.uppercase()
                val brandWords = brand.split(" ").filter { it.length > 1 }
                if (brandWords.isEmpty()) continue
                
                var foundWords = 0
                for (word in brandWords) {
                    // Check if word exists as a whole word in the cleaned OCR text
                    if (cleanedTextForSearch.contains("\\b${Regex.escape(word)}\\b".toRegex())) {
                        foundWords++
                    }
                }
                
                if (foundWords == 0) continue
                
                val wordMatchRatio = foundWords.toDouble() / brandWords.size
                
                // Prioritize full brand matches (e.g. "NEOZEP FORTE") over partial matches ("NEOZEP")
                var score = wordMatchRatio * 5000.0 + (med.brandName.length * 10.0)
                
                // Huge boost for matching all words of a brand
                if (wordMatchRatio == 1.0) score += 10000.0
                
                // Boost if the full brand string is found exactly as a substring
                if (cleanedTextForSearch.contains(brand)) score += 5000.0
                
                if (score > highestScore) {
                    highestScore = score
                    bestMatch = med
                }
            }

            detectedExpiryDate = parseExpiryDate(fullRawText)
            detectedDosage = parseDosage(fullRawText)

            withContext(Dispatchers.Main) {
                graphicOverlay.clear()
                graphicOverlay.setTransformationInfo(width, height, rotationDegrees)
                for (block in blocks) {
                    val cleanText = block.text.replace("[^A-Za-z0-9 ]".toRegex(), " ")
                    val isMatch = database.medicineDao().findMatchingMedicine(cleanText) != null
                    graphicOverlay.add(GraphicOverlay.TextGraphic(graphicOverlay, block, isMatch))
                }

                if (bestMatch != null) {
                    tvMedicineName.text = bestMatch.brandName
                    tvGenericName.text = bestMatch.genericName
                    
                    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    val descriptionText = buildString {
                        if (detectedDosage.isNotEmpty()) append("Dosage: $detectedDosage\n")
                        if (detectedExpiryDate > 0) append("Expiry detected: ${sdf.format(Date(detectedExpiryDate))}\n")
                        append(bestMatch.description ?: "")
                    }
                    
                    tvDescription.text = descriptionText
                    bottomSheet.visibility = View.VISIBLE
                    graphicOverlay.stopAnimation()
                } else {
                    Toast.makeText(this@ScanActivity, "No medicine detected.", Toast.LENGTH_LONG).show()
                    resetScanner()
                }
            }
        }
    }

    private fun parseDosage(text: String): String {
        // Fix potential O/0 confusion and standardize
        val normalized = text.uppercase()
            .replace("(?<=\\d)O|O(?=\\d)".toRegex(), "0")
            .replace("\\bI\\b|\\bl\\b".toRegex(), "1")

        val dosagePattern = Regex("(\\d+(?:\\.\\d+)?)\\s*(MG|mcg|ml|g|IU)", RegexOption.IGNORE_CASE)
        val matches = dosagePattern.findAll(normalized)
        
        // Strict deduplication using standardized strings to avoid duplicate dosages from multiple tablets
        val uniqueDosages = linkedSetOf<String>()
        matches.forEach { match ->
            val valueStr = match.groupValues[1].trim()
            val value = valueStr.toDoubleOrNull() ?: return@forEach
            // Standardize number format: "10.0" -> "10"
            val formattedValue = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
            
            val unit = match.groupValues[2].uppercase().trim()
            uniqueDosages.add("$formattedValue $unit")
        }
        
        return uniqueDosages.joinToString(" / ")
    }

    private fun parseExpiryDate(text: String): Long {
        try {
            // Aggressive cleaning to handle noisy prints and special symbols like bullets/dots
            val cleaned = text.uppercase()
                .replace("(?<=\\d)O|O(?=\\d)".toRegex(), "0")
                .replace("\n", " ")
                .replace("[^A-Z0-9 ]".toRegex(), " ") // photo shows a bullet/dot, replace with space
                .replace("\\s+".toRegex(), " ")
            
            val monthsRegex = "JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC"
            val monthMap = mapOf(
                "JAN" to 0, "FEB" to 1, "MAR" to 2, "APR" to 3, "MAY" to 4, "JUN" to 5,
                "JUL" to 6, "AUG" to 7, "SEP" to 8, "OCT" to 9, "NOV" to 10, "DEC" to 11
            )
            
            // Multiple patterns for robustness - handling both JUN 2026 and numeric versions
            val patterns = listOf(
                Regex("($monthsRegex)\\s+(20\\d{2}|\\d{2})"), // e.g. JUN 2026
                Regex("(0[1-9]|1[0-2])\\s+(20\\d{2}|\\d{2})"), // e.g. 06 2026
                Regex("(20\\d{2}|\\d{2})\\s+($monthsRegex)")  // e.g. 2026 JUN
            )
            
            for (pattern in patterns) {
                val match = pattern.find(cleaned)
                if (match != null) {
                    val g1 = match.groupValues[1]
                    val g2 = match.groupValues[2]
                    
                    val month: Int
                    val year: Int
                    
                    if (g1.matches(Regex(monthsRegex))) {
                        month = monthMap[g1] ?: 0
                        year = g2.toIntOrNull() ?: continue
                    } else if (g1.matches(Regex("(0[1-9]|1[0-2])"))) {
                        month = g1.toInt() - 1
                        year = g2.toIntOrNull() ?: continue
                    } else { // Year first case
                        year = g1.toIntOrNull() ?: continue
                        month = monthMap[g2] ?: (g2.toIntOrNull()?.minus(1) ?: 0)
                    }
                    
                    val finalYear = if (year < 100) year + 2000 else year
                    // Basic sanity check for year range
                    if (finalYear in 2020..2045) {
                        val calendar = Calendar.getInstance()
                        calendar.set(finalYear, month.coerceIn(0, 11), 1, 0, 0, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        return calendar.timeInMillis
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScanActivity", "Expiry parse error", e)
        }
        return 0
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
        private val REQUIRED_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }
}
