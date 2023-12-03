package com.tutorial.mobile.spinefairy.activity.stats

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.model.PoseMeasurement
import com.tutorial.mobile.spinefairy.utils.measurementsFromCsv
import kotlin.math.roundToInt

class StatsActivity : ComponentActivity() {
    private lateinit var stats: List<PoseMeasurement>
    private var threshold: Float = 1f

    private lateinit var chart: LineChart
    private lateinit var textView: TextView

    companion object {
        const val STATS_PERMISSION_CODE = 30
        const val STAT_EXTRA = "stat_extra"
        const val THRESHOLD_EXTRA = "threshold_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ),
            STATS_PERMISSION_CODE
        )
        setContentView(R.layout.activity_stats)

        textView = findViewById(R.id.statText)
        intent.getStringExtra(STAT_EXTRA)?.let {
            stats = measurementsFromCsv(it)
        }
        threshold = intent.getFloatExtra(THRESHOLD_EXTRA, 0f)

        chart = findViewById(R.id.chart)
        chart.data = LineData()
        drawChart()
        writeText()
    }

    private fun drawChart() {
        val data = chart.data
        LineDataSet(null, "head position").apply {
            color = Color.rgb(64, 237, 218)
            setDrawCircles(false)
            setDrawValues(false)
            data.addDataSet(this)
        }
        LineDataSet(null, "shoulders position").apply {
            color = Color.rgb(198, 131, 215)
            setDrawCircles(false)
            setDrawValues(false)
            data.addDataSet(this)
        }
        val headPosAvg = stats.map { it.neckToNose }.average().toFloat()
        val shouldersPosAvg = stats.map { 1 / it.shouldersDist }.average().toFloat()
        stats.forEachIndexed { i, stat ->
            data.addEntry(Entry(i.toFloat(), stat.neckToNose / headPosAvg), 0)
            data.addEntry(Entry(i.toFloat(), (1 / stat.shouldersDist) / shouldersPosAvg), 1)
        }
        chart.xAxis.setDrawLabels(false)
        chart.axisLeft.setDrawLabels(false)
        chart.axisRight.setDrawLabels(false)
        chart.description.text = ""
        chart.legend.textColor = Color.WHITE
        chart.notifyDataSetChanged()
        chart.setVisibleXRange(0f, stats.size.toFloat())
//        chart.moveViewToX(timestamp.toFloat())
    }

    private fun writeText() {
        val badPostureRatio = stats.count { it.neckToNose > threshold }.toFloat() /
                stats.size
        val text = "you've had bad pose ${(badPostureRatio * 100).roundToInt()}% of entire session"
        textView.text = text
    }
}