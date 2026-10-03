package com.muscom.app.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class Cue { DEAL, MY_TURN, BET, ORDAGO, WIN, LOSE, TAP }

/**
 * Sonido y háptica. El prototipo usa tonos del sistema (sin assets con derechos);
 * en fase 3 se sustituyen por efectos propios con SoundPool.
 */
@Singleton
class Feedback @Inject constructor(@ApplicationContext private val context: Context) {
    var soundEnabled = true
    var hapticsEnabled = true

    private val tone: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull()
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun play(cue: Cue) {
        if (soundEnabled) {
            val (t, ms) = when (cue) {
                Cue.DEAL -> ToneGenerator.TONE_PROP_ACK to 60
                Cue.MY_TURN -> ToneGenerator.TONE_PROP_BEEP to 80
                Cue.BET -> ToneGenerator.TONE_PROP_PROMPT to 90
                Cue.ORDAGO -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 300
                Cue.WIN -> ToneGenerator.TONE_CDMA_CONFIRM to 400
                Cue.LOSE -> ToneGenerator.TONE_PROP_NACK to 250
                Cue.TAP -> ToneGenerator.TONE_PROP_BEEP2 to 30
            }
            runCatching { tone?.startTone(t, ms) }
        }
        if (hapticsEnabled) {
            val effect = when (cue) {
                Cue.ORDAGO -> VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 160), -1)
                Cue.WIN -> VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40, 50, 120), -1)
                Cue.MY_TURN -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                Cue.BET -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                else -> null
            }
            if (effect != null) runCatching { vibrator?.vibrate(effect) }
        }
    }
}
