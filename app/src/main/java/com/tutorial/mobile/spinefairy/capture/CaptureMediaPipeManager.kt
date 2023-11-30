package com.tutorial.mobile.spinefairy.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
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
import com.tutorial.mobile.spinefairy.model.PoseMarkerResultBundle

class CaptureMediaPipeManager(
    private val context: Context,
    private val onCaptureResult: (res: PoseMarkerResultBundle) -> Unit
) {
    private var poseLandmarker: PoseLandmarker? = null

    companion object {
        private const val TAG = "capture-mediapipe-manager"
        private const val MODEL_PATH = "pose_landmarker_heavy.task"

        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.8f
        private const val MIN_POSE_TRACKING_CONFIDENCE = 0.8f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.5f
    }

    init {
        setup()
    }


    private fun setup() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            .setDelegate(Delegate.GPU)
            .build()
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

        try {
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(CaptureActivity.TAG, e.message ?: "Error while creating poseLandmarker!")
        }

    }

    // Convert the ImageProxy to MP Image and feed it to PoselandmakerHelper.
    fun detectLiveStream(
        imageProxy: ImageProxy,
    ) {
        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )

        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            // Rotate the frame received from the camera to be in the same direction as it'll be shown
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image if user use front camera
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

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

//        Log.i(TAG, poseLandmarker?.toString() ?: "null")
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
        onCaptureResult(resultBundle)
    }
}