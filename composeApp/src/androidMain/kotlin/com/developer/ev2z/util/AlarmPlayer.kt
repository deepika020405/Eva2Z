////package com.developer.ev2z.util
////
////import android.content.Context
////import android.media.AudioAttributes
////import android.media.MediaPlayer
////import com.developer.ev2z.R
////
////object AlarmPlayer {
////
////    private var mediaPlayer: MediaPlayer? = null
////
////    fun play(context: Context) {
////        try {
////            println("🔥 AlarmPlayer.play() called")
////
////            stop() // ensure clean state
////
////            mediaPlayer = MediaPlayer().apply {
////
////                setAudioAttributes(
////                    AudioAttributes.Builder()
////                        .setUsage(AudioAttributes.USAGE_ALARM)
////                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
////                        .build()
////                )
////
////                val afd = context.resources.openRawResourceFd(R.raw.geofence_audio)
////                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
////                afd.close()
////
////                isLooping = true
////
////                prepare()
////                start()
////            }
////
////            println("✅ Alarm started")
////
////        } catch (e: Exception) {
////            println("❌ Alarm error: ${e.message}")
////            e.printStackTrace()
////        }
////    }
////
////    fun stop() {
////        try {
////            mediaPlayer?.stop()
////            mediaPlayer?.release()
////        } catch (_: Exception) {
////        }
////        mediaPlayer = null
////    }
////}
//
//
//package com.developer.ev2z.util
//
//import android.content.Context
//import android.media.MediaPlayer
//import com.developer.ev2z.R
//import com.developer.ev2z.AppContextHolder
//
//actual object AlarmPlayer {
//
//    private var mediaPlayer: MediaPlayer? = null
//
//    actual fun play() {
//        val context: Context = AppContextHolder.context
//
//        if (mediaPlayer == null) {
//            mediaPlayer = MediaPlayer.create(context, R.raw.geofence_audio)
//            mediaPlayer?.isLooping = true
//        }
//        mediaPlayer?.start()
//    }
//
//    actual fun stop() {
//        mediaPlayer?.release()
//        mediaPlayer = null
//    }
//}