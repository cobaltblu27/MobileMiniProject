package com.tutorial.mobile.spinefairy.activity.stats

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.model.PoseMeasurement
import com.tutorial.mobile.spinefairy.utils.measurementsFromCsv

class StatsActivity : ComponentActivity() {
    private lateinit var stats: List<PoseMeasurement>

    companion object {
        const val STATS_PERMISSION_CODE = 30
        const val EXTRA = "stat_extra"
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
        intent.getStringExtra(EXTRA)?.let {
            stats = measurementsFromCsv(it)
        }
        setContentView(R.layout.activity_stats)
    }
}