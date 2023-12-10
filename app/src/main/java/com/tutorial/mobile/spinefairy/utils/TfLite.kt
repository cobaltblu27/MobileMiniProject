package com.tutorial.mobile.spinefairy.utils

import android.app.Activity
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Arrays

fun loadModelFile(activity: Activity, modelName: String): MappedByteBuffer {
    val fileDescriptor = activity.assets.openFd(modelName)
    val fileChannel = FileInputStream(fileDescriptor.fileDescriptor).channel
    return fileChannel.map(
        FileChannel.MapMode.READ_ONLY,
        fileDescriptor.startOffset,
        fileDescriptor.declaredLength
    )
}

fun logModelInfo(TAG: String, interpreter: Interpreter) {
    val inputCnt = interpreter.inputTensorCount
    (0 until inputCnt).map { i ->
        val inputTensor = interpreter.getInputTensor(i)
        val name = inputTensor.name()
        val shape = Arrays.toString(inputTensor.shape())
        val dtype = inputTensor.dataType()
        Log.d(TAG, "input: $i name: $name shape: $shape dtype: $dtype")
    }
    val outputCnt = interpreter.outputTensorCount
    (0 until outputCnt).map { i ->
        val outputTensor = interpreter.getOutputTensor(i)
        val name = outputTensor.name()
        val shape = Arrays.toString(outputTensor.shape())
        val dtype = outputTensor.dataType()
        Log.d(TAG, "output: $i name: $name shape: $shape dtype: $dtype")
    }
}

fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val bytes = ByteArray(remaining())
    get(bytes)
    return bytes
}

fun loadImagenetLabels(activity: Activity): List<String> {
    val assetManager = activity.assets
    return assetManager.open("imagenet_labels.txt").bufferedReader().use { it.readLines() }
}

fun loadActivityLabels(activity: Activity): List<String> {
    val assetManager = activity.assets
    return assetManager.open("activity_labels.txt").bufferedReader().use { it.readLines() }
}

