package com.developer.ev2z

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAppContext(this)  // ⭐ MUST
    }
}