package com.tutorial.mobile.spinefairy.activity.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.tutorial.mobile.spinefairy.activity.capture.CaptureActivity
import com.tutorial.mobile.spinefairy.model.PoseMarkerResultBundle

class CaptureMediaPipeManager(
    private val context: Context,
) {
    private var poseLandmarker: PoseLandmarker? = null
    val onCaptureResult: MutableList<(res: PoseMarkerResultBundle) -> Unit> =
        mutableListOf()

    companion object {
        private const val TAG = "capture-mediapipe-manager"
        private const val MODEL_PATH = "pose_landmarker_heavy.task"

        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.8f
        private const val MIN_POSE_TRACKING_CONFIDENCE = 0.8f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.5f

        const val INTENT_NAME = "capture_mediapipe_manager"
    }

    init {
        setup()
    }


    private fun setup() {
        Log.i("PIPE_MANAGER", "starting setup")
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            .setDelegate(Delegate.GPU)
            .build()
        Log.i("PIPE_MANAGER", "options")
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener { error ->
                Log.e(TAG, error.toString())
            }
            .setMinPoseDetectionConfidence(MIN_POSE_DETECTION_CONFIDENCE)
            .setMinTrackingConfidence(MIN_POSE_TRACKING_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_POSE_PRESENCE_CONFIDENCE)
            .build()
        Log.i("PIPE_MANAGER", "createFromOptions")
        try {
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.i("PIPE_MANAGER", "complete")
        } catch (e: Exception) {
            Log.e(CaptureActivity.TAG, e.message ?: "Error while creating poseLandmarker!")
        }

    }

    fun detectLiveStream(
        imageProxy: ImageProxy,
    ) {
        val frameTime = SystemClock.uptimeMillis()

        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )

        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            postScale(
                -1f,
                1f,
                imageProxy.width.toFloat(),
                imageProxy.height.toFloat()
            )
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        poseLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {

        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        val resultBundle = PoseMarkerResultBundle(
            result,
            inferenceTime,
            input.height,
            input.width
        )
        onCaptureResult.forEach { it(resultBundle) }
    }
}