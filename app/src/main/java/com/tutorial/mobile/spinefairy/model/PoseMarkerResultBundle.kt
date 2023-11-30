package com.tutorial.mobile.spinefairy.model

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

data class PoseMarkerResultBundle(
    val result: PoseLandmarkerResult,
    val inferenceTime: Long,
    val inputImageHeight: Int,
    val inputImageWidth: Int,
)
