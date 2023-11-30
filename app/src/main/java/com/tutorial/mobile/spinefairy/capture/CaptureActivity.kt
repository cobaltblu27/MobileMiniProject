package com.tutorial.mobile.spinefairy.capture

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.model.PoseMarkerResultBundle
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : ComponentActivity() {
    private lateinit var poseLandmarker: PoseLandmarker

    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageAnalyzer: ImageAnalysis

    private lateinit var preview: Preview
    private lateinit var cameraButton: ImageButton
    private lateinit var cameraPreview: PreviewView
    private lateinit var captureCanvasView: CaptureCanvasView
    private lateinit var confidenceText: TextView

    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var captureMediaPipeManager: CaptureMediaPipeManager

    private val resultList : MutableList<PoseMarkerResultBundle> = mutableListOf()

    companion object {
        const val CAMERA_PERMISSION_CODE = 936
        const val TAG = "activity-object-classification"

        const val RESULT_LIST_SIZE = 15
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
        setContentView(R.layout.activity_capture)

        cameraPreview = findViewById(R.id.previewView)
        cameraPreview.post { startCamera() }
        captureCanvasView = findViewById(R.id.captureCanvasView)
        confidenceText = findViewById(R.id.objectDetectionConfidenceTextView)
        backgroundExecutor = Executors.newSingleThreadExecutor()

        backgroundExecutor.execute {
            captureMediaPipeManager = CaptureMediaPipeManager(this) {
                if (resultList.size > RESULT_LIST_SIZE) {
                    resultList.removeAt(0)
                }
                resultList += it
                captureCanvasView.setResults(resultList)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                preview = Preview.Builder().build()
                preview.setSurfaceProvider(cameraPreview.surfaceProvider)
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                    .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                imageAnalyzer.setAnalyzer(backgroundExecutor) {
                    if (this::captureMediaPipeManager.isInitialized) {
                        captureMediaPipeManager.detectLiveStream(imageProxy = it)
                    }
                }

                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "This should never be reached", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
}