package com.example.foodhubapp.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes

object AppSoundPlayer {
    fun play(context: Context, @RawRes soundRes: Int) {
        runCatching {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val player = MediaPlayer.create(context.applicationContext, soundRes, attributes, 0) ?: return
            player.setOnCompletionListener(MediaPlayer::release)
            player.setOnErrorListener { value, _, _ -> value.release(); true }
            player.start()
        }
    }
}
