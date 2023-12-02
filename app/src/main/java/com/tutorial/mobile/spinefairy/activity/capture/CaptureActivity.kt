package com.tutorial.mobile.spinefairy.activity.capture

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.activity.common.CaptureMediaPipeManager
import com.tutorial.mobile.spinefairy.activity.measure.MeasureFragment
import com.tutorial.mobile.spinefairy.model.PoseMarkerResultBundle
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : FragmentActivity() {
    private lateinit var poseLandmarker: PoseLandmarker

    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageAnalyzer: ImageAnalysis

    private lateinit var preview: Preview
    private lateinit var cameraButton: ImageButton
    private lateinit var cameraPreview: PreviewView
    private lateinit var captureCanvasView: CaptureCanvasView
    private lateinit var chart: LineChart

    private var measureFragment: MeasureFragment? = null

    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var captureMediaPipeManager: CaptureMediaPipeManager

    private val resultList: MutableList<PoseMarkerResultBundle> = mutableListOf()
    private var captureStartTimestamp = System.currentTimeMillis()

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
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        cameraPreview = findViewById(R.id.previewView)
        cameraPreview.post { startCamera() }
        captureCanvasView = findViewById(R.id.captureCanvasView)
        backgroundExecutor = Executors.newSingleThreadExecutor()
        chart = findViewById(R.id.chart)

        chart.data = LineData()

        backgroundExecutor.execute {
            captureMediaPipeManager = CaptureMediaPipeManager(this)
            captureMediaPipeManager.onCaptureResult.add {
                if (resultList.size > RESULT_LIST_SIZE) {
                    resultList.removeAt(0)
                }
                resultList += it
                captureCanvasView.setResults(resultList)
            }
        }

        captureCanvasView.debugText = findViewById(R.id.captureInfoTextView)
        captureCanvasView.onUpdateDistance.add {
            updateChart(it.neckToNose)
        }
    }

    private fun updateChart(distance: Float) {
        val data = chart.data
        data.getDataSetByIndex(0) ?: LineDataSet(null, "").apply {
            color = Color.rgb(93, 18, 210)
            setDrawCircles(false)
            data.addDataSet(this)
        }
        val timestamp = System.currentTimeMillis() - captureStartTimestamp
        data.addEntry(Entry(timestamp.toFloat(), distance), 0)
        data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(30f * 1000)
        chart.moveViewToX(timestamp.toFloat())
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

    fun onStartCalibration(view: View) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        measureFragment = MeasureFragment()
        fragmentTransaction.add(R.id.fragmentContainerView, measureFragment!!)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()

        captureCanvasView.onUpdateDistance.add(measureFragment!!::updateDistanceList)
    }

    fun onEndCalibration(view: View) {
        supportFragmentManager.popBackStack()
        captureCanvasView.onUpdateDistance.remove(measureFragment!!::updateDistanceList)
    }
}