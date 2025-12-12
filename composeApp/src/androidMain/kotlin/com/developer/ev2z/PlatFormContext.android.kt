package com.developer.ev2z

import android.app.Application
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable


lateinit var appCtx: Application

fun initAppContext(app: Application) {
    appCtx = app
}

actual fun getAppContext(): Any? = appCtx

@Composable
actual fun getPlatformContext(): Any? = LocalContext.current