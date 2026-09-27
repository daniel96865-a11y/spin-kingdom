package de.danielgrebe.spinkingdom.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import de.danielgrebe.spinkingdom.R

enum class Sfx(val res: Int) {
    CLICK(R.raw.sfx_click), SPIN(R.raw.sfx_spin), REEL_STOP(R.raw.sfx_reel_stop), COIN(R.raw.sfx_coin),
    JACKPOT(R.raw.sfx_jackpot), ATTACK(R.raw.sfx_attack), EXPLOSION(R.raw.sfx_explosion), SHIELD(R.raw.sfx_shield),
    DIG(R.raw.sfx_dig), UPGRADE(R.raw.sfx_upgrade), LEVEL_COMPLETE(R.raw.sfx_level_complete), CHEST(R.raw.sfx_chest),
    CARD(R.raw.sfx_card), TICK(R.raw.sfx_tick), FAIL(R.raw.sfx_fail)
}

/**
 * Sound effects (SoundPool), looping background music (MediaPlayer) and haptics.
 * All sounds are synthesized by tools/gen_sounds.py - no third party audio.
 */
class AudioManager(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = HashMap<Sfx, Int>()
    private var music: MediaPlayer? = null

    var sfxEnabled = true
    var vibrationEnabled = true
    var musicEnabled = true
        set(value) { field = value; if (value) startMusic() else stopMusic() }
    private var inForeground = false

    init {
        Sfx.entries.forEach { ids[it] = pool.load(context, it.res, 1) }
    }

    fun play(s: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!sfxEnabled) return
        val id = ids[s] ?: return
        pool.play(id, volume, volume, 1, 0, rate)
    }

    fun vibrate(ms: Long = 30, strong: Boolean = false) {
        if (!vibrationEnabled) return
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        runCatching { v?.vibrate(VibrationEffect.createOneShot(ms, if (strong) 255 else VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    fun onForeground() { inForeground = true; startMusic() }
    fun onBackground() { inForeground = false; stopMusic() }

    private fun startMusic() {
        if (!musicEnabled || !inForeground) return
        if (music == null) {
            music = runCatching { MediaPlayer.create(context, R.raw.music_loop) }.getOrNull()?.apply {
                isLooping = true
                setVolume(0.35f, 0.35f)
            }
        }
        runCatching { if (music?.isPlaying == false) music?.start() }
    }

    private fun stopMusic() {
        runCatching { music?.stop(); music?.release() }
        music = null
    }
}
