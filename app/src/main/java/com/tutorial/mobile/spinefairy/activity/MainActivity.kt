package com.tutorial.mobile.spinefairy.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import com.tutorial.mobile.spinefairy.R
import com.tutorial.mobile.spinefairy.activity.capture.CaptureActivity
import com.tutorial.mobile.spinefairy.activity.common.CaptureMediaPipeManager
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
