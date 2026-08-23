package com.example.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Generador avanzado de efectos hápticos de alta definición y sintetizador
 * de sonidos para una experiencia inmersiva en el juego.
 */
class SoundHapticsHelper(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Pulsación sutil al presionar cualquier tecla del teclado.
     */
    fun playKeyPress() {
        performPredefinedOrFallback(
            predefinedEffect = VibrationEffect.EFFECT_CLICK,
            fallbackMs = 15,
            fallbackAmplitude = 60
        )
    }

    /**
     * Letra correcta: doble pulso háptico crujiente + tono positivo brillante.
     */
    fun playCorrect() {
        scope.launch {
            playTone(ToneGenerator.TONE_PROP_BEEP, 80)
            delay(90)
            playTone(ToneGenerator.TONE_PROP_BEEP2, 110)
        }
        // Patrón háptico de confirmación alegre
        vibratePattern(
            timings = longArrayOf(0, 30, 40, 50),
            amplitudes = intArrayOf(0, 140, 0, 255)
        )
    }

    /**
     * Letra incorrecta: golpe sordo con caída de tono y vibración doble.
     */
    fun playIncorrect() {
        scope.launch {
            playTone(ToneGenerator.TONE_PROP_NACK, 160)
            delay(120)
            playTone(ToneGenerator.TONE_SUP_ERROR, 140)
        }
        // Pulso pesado de impacto
        performPredefinedOrFallback(
            predefinedEffect = VibrationEffect.EFFECT_HEAVY_CLICK,
            fallbackMs = 180,
            fallbackAmplitude = 240
        )
    }

    /**
     * Cambio de turno: pulso de atención nítido.
     */
    fun playTurnChange() {
        playTone(ToneGenerator.TONE_PROP_PROMPT, 70)
        performPredefinedOrFallback(
            predefinedEffect = VibrationEffect.EFFECT_TICK,
            fallbackMs = 35,
            fallbackAmplitude = 100
        )
    }

    /**
     * Victoria: Fanfarria rítmica con pulsaciones sucesivas.
     */
    fun playWin() {
        scope.launch {
            playTone(ToneGenerator.TONE_DTMF_1, 100)
            delay(110)
            playTone(ToneGenerator.TONE_DTMF_5, 120)
            delay(130)
            playTone(ToneGenerator.TONE_DTMF_9, 140)
            delay(150)
            playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        }
        // Cascada háptica de celebración
        vibratePattern(
            timings = longArrayOf(0, 60, 50, 80, 50, 100, 60, 250),
            amplitudes = intArrayOf(0, 120, 0, 180, 0, 220, 0, 255)
        )
    }

    /**
     * Derrota: Tonos descendentes y zumbido de clausura.
     */
    fun playLose() {
        scope.launch {
            playTone(ToneGenerator.TONE_SUP_ERROR, 220)
            delay(200)
            playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 350)
        }
        vibratePattern(
            timings = longArrayOf(0, 150, 80, 300),
            amplitudes = intArrayOf(0, 200, 0, 120)
        )
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            tg.startTone(toneType, durationMs)
        } catch (e: Exception) {
            Log.d("SoundHapticsHelper", "Audio tone error: ${e.message}")
        }
    }

    private fun performPredefinedOrFallback(
        predefinedEffect: Int,
        fallbackMs: Long,
        fallbackAmplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE
    ) {
        try {
            val vib = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(predefinedEffect))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(fallbackMs, fallbackAmplitude))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(fallbackMs)
            }
        } catch (e: Exception) {
            Log.d("SoundHapticsHelper", "Haptics error: ${e.message}")
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            val vib = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.d("SoundHapticsHelper", "Pattern vibration error: ${e.message}")
        }
    }
}

