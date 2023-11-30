package com.tutorial.mobile.spinefairy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import com.tutorial.mobile.spinefairy.capture.CaptureActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun toStartCapture(view: View) {
        val intent = Intent(this, CaptureActivity::class.java)
        startActivity(intent)
    }
}
