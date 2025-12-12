package com.developer.ev2z

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.developer.ev2z.data.local.initStorageManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize storage for Android
        initStorageManager(this)


        setContent {
            App()
        }
    }
}
