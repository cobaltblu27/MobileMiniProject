package com.tutorial.mobile.spinefairy.activity.measure

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.activity.capture.CaptureCanvasView

class MeasureFragment : Fragment() {
    private val noseDistanceList: MutableList<Float> = mutableListOf()
    private val shoulderLengthList: MutableList<Float> = mutableListOf()

    val averageNoseDist: Float
        get() {
            if (noseDistanceList.size == 0) return 0f
            return noseDistanceList.sum() / noseDistanceList.size
        }
    val averageShoulderLength: Float
        get() {
            if (shoulderLengthList.size == 0) return 0f
            return shoulderLengthList.sum() / shoulderLengthList.size
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_measure, container, false)
    }

    fun updateDistanceList(measurement: CaptureCanvasView.Measurement) {
        noseDistanceList.add(measurement.neckToNose)
        shoulderLengthList.add(measurement.shouldersDist)
    }
}