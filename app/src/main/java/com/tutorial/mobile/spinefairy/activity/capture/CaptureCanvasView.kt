package com.tutorial.mobile.spinefairy.activity.capture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.model.PoseMarkerResultBundle
import com.tutorial.mobile.spinefairy.utils.div
import com.tutorial.mobile.spinefairy.utils.l2
import com.tutorial.mobile.spinefairy.utils.minus
import com.tutorial.mobile.spinefairy.utils.plus
import com.tutorial.mobile.spinefairy.utils.toMk
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.stat.abs
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.map
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.sum
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import org.jetbrains.kotlinx.multik.ndarray.operations.toList
import org.jetbrains.kotlinx.multik.ndarray.operations.unaryMinus
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow


class CaptureCanvasView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    data class Measurement(
        val neckToNose: Float,
        val shouldersDist: Float,
    )

    private var results: List<PoseMarkerResultBundle> = emptyList()

    private var defaultLineColor = ContextCompat.getColor(context!!, R.color.purple_200)
    private var defaultPointColor = ContextCompat.getColor(context!!, R.color.teal_200)

    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var imageWidth = 1
    private var imageHeight = 1
    private var scaleFactor = BASE_SCALE_FACTOR

    private var neckHorizontalOffset: Float? = null

    lateinit var debugText: TextView
    lateinit var chart: LineChart

    val onUpdateDistance: MutableList<(m: Measurement) -> Unit> = mutableListOf()

    companion object {
        private const val TAG = "capture_canvas_view"
        private const val BASE_SCALE_FACTOR = 0.6f
    }

    init {
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = defaultLineColor
        linePaint.strokeWidth = 10f
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = defaultPointColor
        pointPaint.strokeWidth = 10f
        pointPaint.style = Paint.Style.FILL
    }

    private fun calculateNeckToNose(landmarkPointList: List<NormalizedLandmark>): NDArray<Float, D1> {
        val nose = landmarkPointList[0].toMk()
        val shoulder1 = landmarkPointList[11].toMk()
        val shoulder2 = landmarkPointList[12].toMk()

        val neck = (shoulder1 + shoulder2) / 2f
        return nose - neck
    }

    private fun calculateNeckVerticalVector(landmarkPointList: List<NormalizedLandmark>): NDArray<Float, D1> {
        val shoulder1 = landmarkPointList[11].toMk()
        val shoulder2 = landmarkPointList[12].toMk()

        val shoulderLine = shoulder1 - shoulder2
        val arr = mk.ndarray(mk[shoulderLine[1], -shoulderLine[0], 0f])
        return arr / arr.l2()
    }

    private fun calculateShouldersDist(landmarkPointList: List<NormalizedLandmark>): Float {
        val shoulder1 = landmarkPointList[11].toMk()
        val shoulder2 = landmarkPointList[12].toMk()

        val shoulderLine = shoulder1 - shoulder2
        return shoulderLine.l2()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.lastOrNull()?.let { resultBundle ->
            val result = resultBundle.result
            val landmarks = result.landmarks().getOrNull(0) ?: return@let
            for (normalizedLandmarkList in result.landmarks()) {
                for (normalizedLandmark in normalizedLandmarkList) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    drawLine(
                        canvas,
                        result.landmarks()[0][it.start()].toMk(),
                        result.landmarks()[0][it.end()].toMk(),
                    )
                }
            }
            val neckToNose = calculateNeckToNose(landmarks)
            val nose = result.landmarks()[0]?.get(0)?.toMk() ?: return
            val neck = nose - neckToNose
            val neckVerticalVector = calculateNeckVerticalVector(landmarks)
            val neckVertical = -neckVerticalVector * neckToNose

            drawLine(canvas, neck, neck + neckVertical, Color.RED)
            drawLine(canvas, neck + neckVertical, nose, Color.BLUE)

            val noseHorizontalDistance = (neck + neckVertical - nose).l2()
            val shouldersDist = calculateShouldersDist(landmarks)
            val measurement = Measurement(noseHorizontalDistance, shouldersDist)
            onUpdateDistance.forEach { it(measurement) }

            val text = neckVerticalVector.let { landmark ->
                "x: ${landmark[0]}, y: ${landmark[1]}, z: ${landmark[2]}"
            }
            debugText.text = text
//            Log.i(TAG, text)
        }
    }

    private fun drawLine(
        canvas: Canvas,
        start: NDArray<Float, D1>,
        end: NDArray<Float, D1>,
        color: Int = defaultLineColor
    ) {
        linePaint.color = color
        canvas.drawLine(
            start[0] * imageWidth * scaleFactor,
            start[1] * imageHeight * scaleFactor,
            end[0] * imageWidth * scaleFactor,
            end[1] * imageHeight * scaleFactor,
            linePaint
        )
        linePaint.color = defaultLineColor
    }

    fun setResults(value: List<PoseMarkerResultBundle>) {
        if (value.isEmpty()) return
        results = value
        imageWidth = results.first().inputImageWidth
        imageHeight = results.first().inputImageHeight
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight) *
                BASE_SCALE_FACTOR
        invalidate()
    }
}