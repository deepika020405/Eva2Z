//package com.developer.ev2z  // ✅ util hata do
//
//import android.app.NotificationManager
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import com.developer.ev2z.util.AlarmPlayer
//import com.developer.ev2z.util.NotificationUtil
//
//class AlarmStopReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        println("🛑 AlarmStopReceiver.onReceive() CALLED")
//
//        // ✅ Alarm stop
//        AlarmPlayer.stop()
//
//        // ✅ Notification bhi dismiss karo
//        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
//                as NotificationManager
//        manager.cancel(NotificationUtil.GEOFENCE_NOTIFICATION_ID)
//
//        println("✅ Alarm stopped + notification dismissed")
//    }
//}