package com.tutorial.mobile.spinefairy.model

import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get

data class PoseLog(
    val landmarks: List<NDArray<Float, D1>>,
    val badPose: Boolean
) : CsvData<PoseLog> {
    override val header: String
        get() = landmarks.flatMapIndexed { i, _ ->
            listOf(
                "x${i}",
                "y${i}",
                "z${i}"
            )
        }.let {
            it + listOf("badPose")
        }.joinToString(",")

    override fun toCsv(): String {
        return landmarks.flatMap { landmark ->
            listOf(
                landmark[0],
                landmark[1],
                landmark[2]
            ).map { it.toString() }
        }.let {
            it + listOf(badPose.toString())
        }.joinToString(",")
    }
}