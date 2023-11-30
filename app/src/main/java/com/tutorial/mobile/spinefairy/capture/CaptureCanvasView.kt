package com.tutorial.mobile.spinefairy.capture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.model.PoseMarkerResultBundle
import kotlin.math.max


class CaptureCanvasView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: List<PoseMarkerResultBundle> = emptyList()

    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var imageWidth = 1
    private var imageHeight = 1
    private var scaleFactor = 1f

    companion object {
        private const val TAG = "capture_canvas_view"
    }

    init {
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.purple_200)
        linePaint.strokeWidth = 10f
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = ContextCompat.getColor(context!!, R.color.teal_200)
        pointPaint.strokeWidth = 10f
        pointPaint.style = Paint.Style.FILL
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        Log.i(TAG, "draw called")
        results.lastOrNull()?.let { resultBundle ->
            Log.i(TAG, "drawing result")
            val result = resultBundle.result
            for (landmark in result.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        result.landmarks()[0][it!!.start()].x() * imageWidth * scaleFactor,
                        result.landmarks()[0][it.start()].y() * imageHeight * scaleFactor,
                        result.landmarks()[0][it.end()].x() * imageWidth * scaleFactor,
                        result.landmarks()[0][it.end()].y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }
        }
    }

    fun setResults(value: List<PoseMarkerResultBundle>) {
        if (value.isEmpty()) return
        results = value
        imageWidth = results.first().inputImageWidth
        imageHeight = results.first().inputImageHeight
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }
}