package com.tutorial.mobile.spinefairy.activity.measure

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.activity.capture.CaptureActivity

class MeasureActivity :ComponentActivity() {

    companion object {
        const val CAMERA_PERMISSION_CODE = 936
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
        setContentView(R.layout.activity_measure)
    }
}