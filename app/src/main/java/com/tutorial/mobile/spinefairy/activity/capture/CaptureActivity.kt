package com.tutorial.mobile.spinefairy.activity.capture

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
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
import com.tutorial.mobile.spinefairy.activity.stats.StatsActivity
import com.tutorial.mobile.spinefairy.filePath
import com.tutorial.mobile.spinefairy.model.PoseMarkerResultBundle
import com.tutorial.mobile.spinefairy.model.PoseMeasurement
import com.tutorial.mobile.spinefairy.utils.saveCsv
import com.tutorial.mobile.spinefairy.utils.toCsv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private lateinit var guideText: TextView

    private var measureFragment: MeasureFragment? = null

    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var captureMediaPipeManager: CaptureMediaPipeManager

    private val resultList: MutableList<PoseMarkerResultBundle> = mutableListOf()
    private var captureStartTimestamp = System.currentTimeMillis()

    private var measuredShoulderLength: Float = 0.5f
    private var measuredNoseDistance: Float = 0.6f
    private var warnSensitivity: Float = 0.2f
    private val measurementList: MutableList<PoseMeasurement> = mutableListOf()

    companion object {
        const val CAMERA_PERMISSION_CODE = 936
        const val TAG = "activity-object-classification"

        const val RESULT_LIST_SIZE = 15

        const val MEASUREMENT_SMOOTHING_SAMPLES = 8
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

        guideText = findViewById(R.id.captureInfoTextView)
        captureCanvasView.onUpdateDistance.add {
            updateChart(it.neckToNose)
            checkPosture(it)
        }
    }

    private fun updateChart(distance: Float) {
        val data = chart.data
        data.getDataSetByIndex(0) ?: LineDataSet(null, "").apply {
            color = Color.rgb(64, 237, 218)
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

    fun onOpenSettings(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set warning sensitivity")
        val seekbar = SeekBar(this).apply {
            max = 10
            min = 1
            progress = 5
        }
        builder.setView(seekbar)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.setPositiveButton("Submit") { dialog, _ ->
            Log.e(TAG, "${seekbar.progress}") // TODO:
            warnSensitivity = 0.04f * seekbar.progress
            dialog.cancel()
        }
        builder.show()

//        val input = EditText(this)
//        input.inputType = InputType.TYPE_CLASS_TEXT
//        builder.setView(input)
//        builder.setPositiveButton("OK") { _, _ ->
//            RadioButton(this)
//                .apply { text = input.text.toString() }
//                .let { labelsRadioGroup.addView(it) }
//        }
    }

    private fun checkPosture(measurement: PoseMeasurement) {
        measurementList.add(measurement)
        val measurementSubset = measurementList.takeLast(MEASUREMENT_SMOOTHING_SAMPLES)
        val shoulderLength = measurementSubset.map { it.shouldersDist }.sum() /
                MEASUREMENT_SMOOTHING_SAMPLES
        val neckDistance = measurementSubset.map { it.neckToNose }.sum() /
                MEASUREMENT_SMOOTHING_SAMPLES
        val bodyRelativeDistance = shoulderLength / measuredShoulderLength
        Log.i(TAG, "rel: ${bodyRelativeDistance}")
        if (neckDistance > measuredNoseDistance * (1 + warnSensitivity)) {
            guideText.text = "Sit straight!"
        } else {
            guideText.text = ""
        }
    }

    fun onStartCalibration(view: View) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        measureFragment = MeasureFragment()
        fragmentTransaction.add(R.id.fragmentContainerView, measureFragment!!)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()

        captureCanvasView.onUpdateDistance.add(measureFragment!!::updateDistanceList)

        CoroutineScope(Dispatchers.Main).launch {
            delay(10000)
            endCalibration()
        }
    }

    private fun endCalibration() {
        measuredNoseDistance = measureFragment?.averageNoseDist ?: measuredNoseDistance
        measuredShoulderLength = measureFragment?.averageShoulderLength
            ?: measuredShoulderLength
        Log.i(TAG, "Nose: $measuredNoseDistance, Shoulder: $measuredShoulderLength")
        supportFragmentManager.popBackStack()
        captureCanvasView.onUpdateDistance.remove(measureFragment!!::updateDistanceList)
    }

    fun toStats(view: View) {
        val intent = Intent(this, StatsActivity::class.java)
        saveMeasurements()
        intent.putExtra(StatsActivity.EXTRA, measurementList.toCsv())
        startActivity(intent)
    }

    private fun saveMeasurements() {
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val dateStr = dateFormat.format(currentDateTime)
        val fileName = "posture_stat_$dateStr.csv"
        saveCsv(measurementList.toCsv(), fileName)
    }
}