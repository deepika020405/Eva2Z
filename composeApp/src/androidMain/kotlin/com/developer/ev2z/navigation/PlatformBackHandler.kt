package com.developer.ev2z.navigation

// androidMain

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.developer.ev2z.getAppContext

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    BackHandler(enabled = enabled) {
        onBack()
    }
}

//actual fun exitApp() {
//    val ctx = getAppContext()
//    if (ctx is Activity) {
//        ctx.finishAffinity()   // ✅ closes all activities
//    }
//}

//actual fun exitApp() {
//    val ctx = getAppContext()
//    if (ctx is Activity) {
//        ctx.finishAffinity()
//        android.os.Process.killProcess(android.os.Process.myPid())
//    }
//}


// androidMain
actual fun exitApp(activity: Any?) {
    if (activity is Activity) {
        activity.moveTaskToBack(true)
    }
}