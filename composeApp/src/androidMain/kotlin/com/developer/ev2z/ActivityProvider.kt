// ActivityProvider.kt (commonMain ya androidMain me)
package com.developer.ev2z

import android.app.Activity

object ActivityProvider {
    private var currentActivity: Activity? = null

    fun setActivity(activity: Activity) {
        currentActivity = activity
    }

    fun getActivity(): Activity? = currentActivity

    fun clearActivity() {
        currentActivity = null
    }
}