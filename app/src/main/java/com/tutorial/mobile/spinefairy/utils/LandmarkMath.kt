package com.tutorial.mobile.spinefairy.utils

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.operations.toList
import kotlin.math.pow

operator fun NormalizedLandmark.plus(l2: NormalizedLandmark): NormalizedLandmark {
    return NormalizedLandmark.create(
        x() + l2.x(),
        y() + l2.y(),
        z() + l2.z(),
    )
}

operator fun NormalizedLandmark.minus(l2: NormalizedLandmark): NormalizedLandmark {
    return this + (l2 * -1f)
}

operator fun NormalizedLandmark.div(f: Float): NormalizedLandmark {
    return NormalizedLandmark.create(
        x() / f,
        y() / f,
        z() / f,
    )
}

operator fun NormalizedLandmark.times(f: Float): NormalizedLandmark {
    return NormalizedLandmark.create(
        x() * f,
        y() * f,
        z() * f,
    )
}

fun NormalizedLandmark.toMk(): D1Array<Float> {
    return mk.ndarray(mk[x(), y(), z()])
}

fun D1Array<Float>.l2(): Float {
    return toList().map { it.pow(2) }.sum().pow(0.5f)
}
