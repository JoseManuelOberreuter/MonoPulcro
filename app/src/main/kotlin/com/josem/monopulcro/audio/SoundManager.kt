package com.josem.monopulcro.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SoundManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val popSoundId: Int
    private val cashRegisterSoundId: Int

    init {
        popSoundId = loadSound(SOUND_POP)
        cashRegisterSoundId = loadSound(SOUND_CASH_REGISTER)
    }

    private fun loadSound(assetPath: String): Int = try {
        appContext.assets.openFd(assetPath).use { afd -> soundPool.load(afd, 1) }
    } catch (_: Exception) {
        0
    }

    /** Campanita al splash; no bloquea la UI */
    fun playIntroJingle() {
        scope.launch { playAndAwait(SOUND_INTRO) }
    }

    /** Pop instantáneo al marcar; SoundPool precargado */
    fun playTaskPop() {
        if (popSoundId != 0) {
            soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    /** Caja registradora al comprar en la tienda */
    fun playCashRegister() {
        if (cashRegisterSoundId != 0) {
            soundPool.play(cashRegisterSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    /** Spray termina → window_cleaning al instante. */
    fun playCleaningSequence() {
        scope.launch {
            playAndAwait(SOUND_SPRAY)
            playAndAwait(SOUND_WINDOW)
        }
    }

    /** Grito del mono durante la celebración de banana */
    fun playMonkeyCheer() {
        scope.launch { playAndAwait(SOUND_CHEER) }
    }

    private suspend fun playAndAwait(assetPath: String) {
        try {
            suspendCancellableCoroutine { cont ->
                val afd = appContext.assets.openFd(assetPath)
                val player = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    setOnCompletionListener {
                        release()
                        if (cont.isActive) cont.resume(Unit)
                    }
                    setOnErrorListener { _, _, _ ->
                        release()
                        if (cont.isActive) cont.resume(Unit)
                        true
                    }
                    start()
                }
                cont.invokeOnCancellation { player.release() }
            }
        } catch (_: Exception) {
            // Sin sonido si falta el asset o falla el reproductor
        }
    }

    companion object {
        private const val SOUND_INTRO  = "sounds/campanitas_intro.mp3"
        private const val SOUND_POP    = "sounds/pop_tarea.mp3"
        private const val SOUND_SPRAY  = "sounds/spray_bottle.mp3"
        private const val SOUND_WINDOW = "sounds/window_cleaning.mp3"
        private const val SOUND_CHEER         = "sounds/grito_mono.mp3"
        private const val SOUND_CASH_REGISTER = "sounds/cash_register.mp3"

        @Volatile
        private var instance: SoundManager? = null

        fun get(context: Context): SoundManager =
            instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
    }
}
