package com.developer.ev2z.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build

object CallSoundPlayer {

    private var ringtone: Ringtone? = null

    fun start(context: Context) {
        if (ringtone?.isPlaying == true) return

        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val rt = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use AudioAttributes — streamType is deprecated on API 28+.
                // USAGE_ALARM bypasses silent/vibrate mode so the call is always audible.
                rt.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                rt.isLooping = true
            } else {
                @Suppress("DEPRECATION")
                rt.streamType = AudioManager.STREAM_ALARM
            }

            // Ensure alarm stream has audible volume (restore after stop).
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val curVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (curVol == 0) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol / 2, 0)
            }

            rt.play()
            ringtone = rt
            println("🔔 CallSoundPlayer started (alarm stream)")
        } catch (e: Exception) {
            println("❌ CallSoundPlayer.start error: ${e.message}")
        }
    }

    fun stop() {
        try {
            ringtone?.stop()
            ringtone = null
            println("🔕 CallSoundPlayer stopped")
        } catch (e: Exception) {
            println("❌ CallSoundPlayer.stop error: ${e.message}")
        }
    }

    val isPlaying: Boolean get() = ringtone?.isPlaying == true
}