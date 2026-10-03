package com.nobodymusic.tyxypoor.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

object PlayerHolder {
    @Volatile
    private var player: ExoPlayer? = null

    fun get(context: Context): ExoPlayer {
        val cur = player
        if (cur != null) return cur
        synchronized(this) {
            val again = player
            if (again != null) return again
            val p = ExoPlayer.Builder(context.applicationContext).build()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            p.setHandleAudioBecomingNoisy(true)
            player = p
            return p
        }
    }
}
