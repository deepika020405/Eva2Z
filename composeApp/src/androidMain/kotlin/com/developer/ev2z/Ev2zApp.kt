package com.developer.ev2z


import android.app.Application

class Ev2zApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ GLOBAL CONTEXT (CMP safe)
        AppContextHolder.context = this
    }
}