package com.example.fitnessapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitnessapp.databinding.FragmentScannerBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Locale
import java.io.ByteArrayOutputStream
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.label.ImageLabel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var detectedCalories: Int = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnCapture.setOnClickListener { takePhoto() }

        binding.btnRetake.setOnClickListener {
            resetScanner()
        }

        binding.btnSave.setOnClickListener {
            saveCaloriesAndExit()
        }
    }

    private fun resetScanner() {
        detectedCalories = 0
        _binding?.let {
            it.ivCapturedImage.visibility = View.GONE
            it.viewFinder.visibility = View.VISIBLE
            it.tvResult.visibility = View.GONE
            it.layoutPostCapture.visibility = View.GONE
            it.btnCapture.visibility = View.VISIBLE
            it.btnCapture.isEnabled = true
        }
    }

    private fun saveCaloriesAndExit() {
        if (detectedCalories > 0) {
            val context = context ?: return
            val prefs = context.getSharedPreferences("FitnessAppPrefs", android.content.Context.MODE_PRIVATE)
            val currentLCal = prefs.getInt("lCal", 0)
            prefs.edit().putInt("lCal", currentLCal + detectedCalories).apply()
            Toast.makeText(context, "Saved $detectedCalories kcal to Lunch!", Toast.LENGTH_SHORT).show()
        }
        parentFragmentManager.popBackStack()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("ScannerFragment", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        _binding?.let {
            it.progressBar.visibility = View.VISIBLE
            it.btnCapture.isEnabled = false
        }

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    
                    _binding?.let {
                        it.ivCapturedImage.setImageBitmap(bitmap)
                        it.ivCapturedImage.visibility = View.VISIBLE
                        it.viewFinder.visibility = View.GONE
                        it.btnCapture.visibility = View.GONE
                    }
                    
                    analyzeImage(bitmap)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("ScannerFragment", "Photo capture failed: ${exc.message}", exc)
                    _binding?.let {
                        it.progressBar.visibility = View.GONE
                        it.btnCapture.isEnabled = true
                        it.tvResult.text = "Camera Error: ${exc.message}"
                        it.tvResult.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    private fun analyzeImage(bitmap: Bitmap) {
        val geminiKey = BuildConfig.GEMINI_API_KEY
        val resizedBitmap = resizeBitmap(bitmap, 768)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                _binding?.let { 
                    it.progressBar.visibility = View.VISIBLE
                    it.tvResult.text = "Scanning food..." 
                    it.tvResult.visibility = View.VISIBLE
                }
                
                // --- ENGINE 1: GEMINI 1.5 FLASH ---
                var resultText: String? = try {
                    val model = GenerativeModel(modelName = "gemini-1.5-flash-latest", apiKey = geminiKey)
                    val response = withContext(Dispatchers.IO) {
                        model.generateContent(content {
                            image(resizedBitmap)
                            text("Identify this food. Provide the name and total calories. Return ONLY [Name]: [Number] kcal. Example: Apple: 95 kcal")
                        })
                    }
                    response.text
                } catch (e: Exception) {
                    null
                }

                if (resultText != null && !resultText.contains("Error") && !resultText.contains("404")) {
                    displayResult(resultText)
                    return@launch
                }

                // --- ENGINE 2: LOCAL FOOD KNOWLEDGE BASE ---
                val labels = getLabelsFromImage(resizedBitmap)
                val generalFoodCals = mapOf(
                    "apple" to 95,
                    "banana" to 105,
                    "bread" to 80,
                    "egg" to 70,
                    "chicken" to 240,
                    "pizza" to 285,
                    "burger" to 350,
                    "salad" to 100,
                    "rice" to 200,
                    "milk" to 150
                )

                var matchedFood = ""
                var totalCals = 0

                // Check labels against our general food list
                for (label in labels) {
                    val labelText = label.text.lowercase()
                    for ((food, cals) in generalFoodCals) {
                        if (labelText.contains(food)) {
                            matchedFood = food.replaceFirstChar { it.uppercase() }
                            totalCals = cals
                            break
                        }
                    }
                    if (totalCals > 0) break
                }

                if (totalCals > 0) {
                    displayResult("$matchedFood: $totalCals kcal")
                } else {
                    // --- ENGINE 3: ML KIT SMART GUESS (New Fallback) ---
                    val topLabel = labels.firstOrNull()?.text?.lowercase() ?: ""
                    val guessCals = when {
                        topLabel.contains("fruit") -> 80
                        topLabel.contains("vegetable") -> 50
                        topLabel.contains("bread") || topLabel.contains("pastry") -> 250
                        topLabel.contains("meat") -> 300
                        topLabel.contains("drink") -> 150
                        else -> 0
                    }

                    if (guessCals > 0) {
                        displayResult("${topLabel.replaceFirstChar { it.uppercase() }}: $guessCals kcal")
                    } else {
                        // Final Fallback to Spoonacular
                        val spoonResponse = withContext(Dispatchers.IO) {
                            try { SpoonacularApiManager.analyzeFoodImage(bitmapToByteArray(resizedBitmap)) } catch (e: Exception) { null }
                        }
                        if (spoonResponse != null && spoonResponse.status == "success") {
                            val name = spoonResponse.category.replace("_", " ").capitalize()
                            val cals = spoonResponse.nutrition.calories.value.toInt()
                            displayResult("$name: $cals kcal")
                        } else {
                            displayError("Could not identify dish. Try a clearer photo.")
                        }
                    }
                }

            } catch (e: Exception) {
                displayError("Please try again.")
            }
        }
    }

    private suspend fun getLabelsFromImage(bitmap: Bitmap): List<ImageLabel> = suspendCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(image)
            .addOnSuccessListener { labels -> cont.resume(labels) }
            .addOnFailureListener { cont.resume(emptyList()) }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    private fun displayResult(text: String) {
        _binding?.let {
            it.tvResult.text = text
            it.tvResult.visibility = View.VISIBLE
            it.layoutPostCapture.visibility = View.VISIBLE
            it.progressBar.visibility = View.GONE
        }
        detectedCalories = parseCalories(text)
    }

    private fun displayError(msg: String) {
        _binding?.let {
            it.progressBar.visibility = View.GONE
            it.btnCapture.isEnabled = true
            it.btnCapture.visibility = View.VISIBLE
            it.tvResult.text = msg
            it.tvResult.visibility = View.VISIBLE
        }
    }

    private fun parseCalories(text: String): Int {
        val caloriePattern = Regex("""(\d+)\s*(?:kcal|calories)""", RegexOption.IGNORE_CASE)
        val match = caloriePattern.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun resizeBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxSize && height <= maxSize) return source

        val aspectRatio: Float = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / aspectRatio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
