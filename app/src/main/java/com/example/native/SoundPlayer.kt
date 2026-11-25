package com.example.native

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Hardened controller for musical habit alarms, preview playback, and custom ringtones.
 * Implements asynchronous audio preparation, cancellable background WAV synthesis,
 * deterministic resource release, lifecycle scoping, and non-blocking fallbacks.
 */
class SoundPlayer(private val context: Context) {

    companion object {
        private const val TAG = "SoundPlayer"
        private const val SAMPLE_RATE = 44100
        private val fileGenerationLocks = ConcurrentHashMap<String, Mutex>()
        private val hasEnsuredBuiltins = AtomicBoolean(false)
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioTrack: AudioTrack? = null
    private var fallbackRingtone: android.media.Ringtone? = null
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private val playerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val playbackSeq = AtomicInteger(0)
    private var playbackJob: Job? = null
    private var completionTimerJob: Job? = null
    @Volatile
    private var isLoopStopped: Boolean = false

    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun requestAlarmAudioFocus() {
        try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val req = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { }
                    .build()
                audioFocusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    android.media.AudioManager.STREAM_ALARM,
                    android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to request alarm audio focus", e)
        }
    }

    private fun abandonAlarmAudioFocus() {
        try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { req ->
                    audioManager.abandonAudioFocusRequest(req)
                }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (_: Throwable) {}
    }

    private fun invokeCompletionOnMain(seq: Int, onCompletion: (() -> Unit)?) {
        if (onCompletion == null) return
        mainHandler.post {
            if (seq == playbackSeq.get() && !isLoopStopped) {
                onCompletion()
            }
        }
    }

    private fun playPcmWithAudioTrack(
        pcmData: ByteArray,
        seq: Int,
        isAlarm: Boolean = false,
        loop: Boolean = false,
        onCompletion: (() -> Unit)? = null
    ) {
        if (seq != playbackSeq.get() || isLoopStopped) return
        stopAudioTrack()
        completionTimerJob?.cancel()
        completionTimerJob = null

        val frameCount = pcmData.size / 2
        if (frameCount <= 0) {
            playFallbackNotification(isAlarm = isAlarm, loop = loop)
            if (!loop) invokeCompletionOnMain(seq, onCompletion)
            return
        }

        try {
            val audioAttrs = AudioAttributes.Builder()
                .setContentType(
                    if (isAlarm) AudioAttributes.CONTENT_TYPE_SONIFICATION else AudioAttributes.CONTENT_TYPE_MUSIC
                )
                .setUsage(
                    if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
                )
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttrs)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val written = track.write(pcmData, 0, pcmData.size)
            if (written <= 0) {
                try { track.release() } catch (_: Throwable) {}
                val fallbackStarted = playFallbackNotification(isAlarm = isAlarm, loop = loop)
                if (!loop) {
                    if (isAlarm && !fallbackStarted) abandonAlarmAudioFocus()
                    invokeCompletionOnMain(seq, onCompletion)
                }
                return
            }

            if (loop) {
                val loopRes = track.setLoopPoints(0, frameCount, -1)
                if (loopRes != AudioTrack.SUCCESS) {
                    Log.w(TAG, "AudioTrack.setLoopPoints returned error $loopRes; using looped fallback ringtone")
                    try { track.release() } catch (_: Throwable) {}
                    playFallbackNotification(isAlarm = isAlarm, loop = true)
                    return
                }
            } else {
                val completionFired = AtomicBoolean(false)
                val markerFrame = maxOf(1, frameCount - 1)
                track.setNotificationMarkerPosition(markerFrame)
                track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack?) {
                        if (completionFired.compareAndSet(false, true)) {
                            completionTimerJob?.cancel()
                            completionTimerJob = null
                            stopAudioTrack()
                            if (isAlarm) abandonAlarmAudioFocus()
                            invokeCompletionOnMain(seq, onCompletion)
                        }
                    }
                    override fun onPeriodicNotification(t: AudioTrack?) {}
                })

                // Backup timer guarantees onCompletion even if HAL drops static marker callback
                val expectedDurationMs = (frameCount * 1000L) / SAMPLE_RATE + 150L
                completionTimerJob = playerScope.launch(Dispatchers.Main) {
                    delay(expectedDurationMs)
                    if (seq == playbackSeq.get() && !isLoopStopped && completionFired.compareAndSet(false, true)) {
                        stopAudioTrack()
                        if (isAlarm) abandonAlarmAudioFocus()
                        onCompletion?.invoke()
                    }
                }
            }
            if (seq != playbackSeq.get() || isLoopStopped) {
                try { track.release() } catch (_: Throwable) {}
                if (isAlarm) abandonAlarmAudioFocus()
                return
            }
            currentAudioTrack = track
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack playback error", e)
            val fallbackStarted = playFallbackNotification(isAlarm = isAlarm, loop = loop)
            if (!loop) {
                if (isAlarm && !fallbackStarted) abandonAlarmAudioFocus()
                invokeCompletionOnMain(seq, onCompletion)
            }
        }
    }

    private fun stopAudioTrack() {
        try {
            val track = currentAudioTrack
            currentAudioTrack = null
            if (track != null) {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (_: Throwable) {}
    }

    private fun stopFallbackRingtone() {
        try {
            val r = fallbackRingtone
            fallbackRingtone = null
            r?.stop()
        } catch (_: Throwable) {}
    }

    private fun releaseMediaPlayerOnly() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            player.setOnPreparedListener(null)
            player.setOnErrorListener(null)
            player.setOnCompletionListener(null)
        } catch (_: Throwable) {}

        playerScope.launch(Dispatchers.IO) {
            try {
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (_: Throwable) {}
                try {
                    player.reset()
                } catch (_: Throwable) {}
                try {
                    player.release()
                } catch (_: Throwable) {}
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer asynchronously", e)
            }
        }
    }

    /**
     * Plays preview asynchronously without blocking the UI thread.
     * Uses zero-failure AudioTrack synthesis for all built-in melodies and
     * safe file descriptor MediaPlayer streaming for imported media.
     */
    fun playPreview(
        uriString: String?,
        vibrate: Boolean = true,
        isAlarm: Boolean = false,
        loop: Boolean = false,
        onCompletion: (() -> Unit)? = null
    ) {
        stopPreview()
        isLoopStopped = false
        val mySeq = playbackSeq.incrementAndGet()

        if (isAlarm) {
            requestAlarmAudioFocus()
        }

        if (vibrate) {
            if (loop && isAlarm) {
                triggerAlarmVibrationLoop()
            } else {
                triggerVibration()
            }
        }

        val builtIn = findBuiltInSoundDef(uriString)
        if (builtIn != null) {
            playbackJob = playerScope.launch(Dispatchers.IO) {
                val durations = if (loop) builtIn.durationsMs + listOf(650) else builtIn.durationsMs
                val freqs = if (loop) builtIn.frequencies + listOf(0f) else builtIn.frequencies
                val pcm = generatePcmDataWithSilence(freqs, durations)
                withContext(Dispatchers.Main) {
                    if (mySeq == playbackSeq.get() && !isLoopStopped) {
                        playPcmWithAudioTrack(pcm, seq = mySeq, isAlarm = isAlarm, loop = loop, onCompletion = onCompletion)
                    }
                }
            }
            return
        }

        playbackJob = playerScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Preparing playback for URI: $uriString (isAlarm=$isAlarm, loop=$loop)")
                val uri = resolveUriSafely(uriString)
                val audioAttrs = AudioAttributes.Builder()
                    .setContentType(
                        if (isAlarm) AudioAttributes.CONTENT_TYPE_SONIFICATION else AudioAttributes.CONTENT_TYPE_MUSIC
                    )
                    .setUsage(
                        if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
                    )
                    .build()

                val mp = MediaPlayer().apply {
                    setAudioAttributes(audioAttrs)
                    isLooping = loop
                    setVolume(1.0f, 1.0f)

                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.exists() && file.length() > 0) {
                            FileInputStream(file).use { fis ->
                                setDataSource(fis.fd, 0, file.length())
                            }
                        } else {
                            setDataSource(appContext, uri)
                        }
                    } else if (uri.scheme == "content") {
                        try {
                            appContext.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                                val statSize = pfd.statSize
                                if (statSize > 0L) {
                                    setDataSource(pfd.fileDescriptor, 0L, statSize)
                                } else {
                                    setDataSource(pfd.fileDescriptor)
                                }
                            } ?: setDataSource(appContext, uri)
                        } catch (_: Exception) {
                            setDataSource(appContext, uri)
                        }
                    } else {
                        setDataSource(appContext, uri)
                    }

                    setOnPreparedListener { player ->
                        if (mySeq != playbackSeq.get() || isLoopStopped) {
                            try {
                                player.release()
                            } catch (_: Throwable) {}
                            return@setOnPreparedListener
                        }
                        Log.d(TAG, "MediaPlayer prepared, starting playback (loop=$loop)")
                        try {
                            player.start()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error starting MediaPlayer onPrepared", e)
                            releaseMediaPlayerOnly()
                            val fallbackStarted = playFallbackNotification(isAlarm = isAlarm, loop = loop)
                            if (!loop) {
                                if (isAlarm && !fallbackStarted) abandonAlarmAudioFocus()
                                invokeCompletionOnMain(mySeq, onCompletion)
                            }
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        releaseMediaPlayerOnly()
                        if (mySeq == playbackSeq.get() && !isLoopStopped) {
                            val fallbackStarted = playFallbackNotification(isAlarm = isAlarm, loop = loop)
                            if (!loop) {
                                if (isAlarm && !fallbackStarted) abandonAlarmAudioFocus()
                                invokeCompletionOnMain(mySeq, onCompletion)
                            }
                        } else if (isAlarm) {
                            abandonAlarmAudioFocus()
                        }
                        true
                    }
                    setOnCompletionListener {
                        Log.d(TAG, "MediaPlayer playback completed")
                        if (!loop) {
                            releaseMediaPlayerOnly()
                            if (isAlarm) abandonAlarmAudioFocus()
                            invokeCompletionOnMain(mySeq, onCompletion)
                        }
                    }
                }
                if (mySeq != playbackSeq.get() || isLoopStopped) {
                    try { mp.release() } catch (_: Throwable) {}
                    if (isAlarm) abandonAlarmAudioFocus()
                    return@launch
                }
                mediaPlayer = mp
                mp.prepareAsync()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing MediaPlayer for: $uriString", e)
                releaseMediaPlayerOnly()
                if (mySeq == playbackSeq.get() && !isLoopStopped) {
                    val fallbackStarted = playFallbackNotification(isAlarm = isAlarm, loop = loop)
                    if (!loop) {
                        if (isAlarm && !fallbackStarted) abandonAlarmAudioFocus()
                        invokeCompletionOnMain(mySeq, onCompletion)
                    }
                } else if (isAlarm) {
                    abandonAlarmAudioFocus()
                }
            }
        }
    }

    private fun generatePcmDataWithSilence(frequencies: List<Float>, durationsMs: List<Int>): ByteArray {
        return synthesizeNotesToPcm(frequencies, durationsMs, checkCancellation = false)
    }

    private fun synthesizeNotesToPcm(
        frequencies: List<Float>,
        durationsMs: List<Int>,
        checkCancellation: Boolean,
        coroutineContext: kotlin.coroutines.CoroutineContext? = null
    ): ByteArray {
        val totalDurationMs = durationsMs.sum()
        val totalSamples = (SAMPLE_RATE * (totalDurationMs / 1000.0)).toInt().coerceAtLeast(0)
        val dataSize = totalSamples * 2
        val pcmData = ByteArray(dataSize)
        var pcmOffset = 0

        val fadeInMax = (SAMPLE_RATE * 0.008).toInt() // 8ms attack taper eliminates start clicks
        val fadeOutMax = (SAMPLE_RATE * 0.015).toInt() // 15ms release taper eliminates boundary pops

        for (noteIdx in frequencies.indices) {
            if (noteIdx >= durationsMs.size) break
            if (checkCancellation && coroutineContext != null) {
                coroutineContext.ensureActive()
            }
            val freq = frequencies[noteIdx]
            val durationMs = durationsMs[noteIdx]
            val noteSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(0)
            if (freq <= 0f) {
                pcmOffset = (pcmOffset + noteSamples * 2).coerceAtMost(dataSize)
                continue
            }

            val fadeInSamples = minOf(fadeInMax, noteSamples / 4).coerceAtLeast(1)
            val fadeOutSamples = minOf(fadeOutMax, noteSamples / 3).coerceAtLeast(1)

            for (i in 0 until noteSamples) {
                if (pcmOffset + 1 >= dataSize) break
                val t = i.toDouble() / SAMPLE_RATE
                val decay = exp(-t * 3.2) * (1.0 - (i.toDouble() / noteSamples) * 0.15)
                val attackTaper = if (i < fadeInSamples) i.toDouble() / fadeInSamples else 1.0
                val samplesRemaining = noteSamples - 1 - i
                val releaseTaper = if (samplesRemaining < fadeOutSamples) samplesRemaining.toDouble() / fadeOutSamples else 1.0
                val envelope = decay * attackTaper * releaseTaper

                val tremolo = 1.0 + 0.08 * sin(2.0 * PI * 6.0 * t)
                val sampleVal = (
                    sin(2.0 * PI * freq * t) * 0.55 +
                    sin(2.0 * PI * (freq * 2.0) * t) * 0.25 +
                    sin(2.0 * PI * (freq * 3.0) * t) * 0.12 +
                    sin(2.0 * PI * (freq * 4.15) * t) * 0.08
                ) * envelope * tremolo * 26000.0

                val shortVal = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                pcmData[pcmOffset++] = (shortVal.toInt() and 0xFF).toByte()
                pcmData[pcmOffset++] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
            }
        }
        return pcmData
    }

    /**
     * Plays an alarm on the Android USAGE_ALARM stream.
     */
    fun playAlarmSound(uriString: String?, vibrate: Boolean = true, loop: Boolean = false) {
        playPreview(uriString, vibrate = vibrate, isAlarm = true, loop = loop)
    }

    /**
     * Plays a continuous looped alarm (audio + optional alarm vibration) until stopAlarmLoop() is called.
     */
    fun playAlarmLoop(uriString: String?, playSound: Boolean, vibrate: Boolean) {
        stopAlarmLoop()
        isLoopStopped = false
        if (playSound) {
            playPreview(uriString, vibrate = vibrate, isAlarm = true, loop = true)
        } else if (vibrate) {
            triggerAlarmVibrationLoop()
        }
    }

    fun stopAlarmLoop() {
        isLoopStopped = true
        stopPreview()
        stopVibration()
        abandonAlarmAudioFocus()
    }

    fun isPlaying(): Boolean {
        return try {
            (currentAudioTrack != null && currentAudioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) ||
                (mediaPlayer?.isPlaying == true) ||
                (fallbackRingtone?.isPlaying == true)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Safe non-blocking fallback using Android system notification/alarm ringtone service.
     */
    private fun playFallbackNotification(isAlarm: Boolean = false, loop: Boolean = false): Boolean {
        return try {
            val ringtoneUri = if (isAlarm) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            val ringtone = ringtoneUri?.let { uri ->
                RingtoneManager.getRingtone(appContext, uri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = loop
                    }
                    fallbackRingtone = this
                    play()
                }
            }
            ringtone != null
        } catch (_: Throwable) {
            false
        }
    }

    fun triggerVibration() {
        try {
            val vibrator = getVibrator() ?: return
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 150, 50, 150)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    fun triggerAlarmVibrationLoop() {
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            val pattern = longArrayOf(0, 500, 250, 500, 750)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val vibAttrs = android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), vibAttrs)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), audioAttrs)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Alarm vibration loop failed", e)
        }
    }

    fun stopVibration() {
        try {
            getVibrator()?.cancel()
        } catch (_: Throwable) {}
    }

    /**
     * Plays celebration chime asynchronously without blocking the UI thread.
     */
    fun playCelebrationFanfare(vibrate: Boolean = true) {
        val dir = File(appContext.filesDir, "musical_ringtones")
        val fanfareFile = File(dir, "triumph_fanfare.wav")
        if (fanfareFile.exists()) {
            playPreview(Uri.fromFile(fanfareFile).toString(), vibrate = false)
        } else {
            playPreview("triumph_fanfare", vibrate = false)
        }
        if (vibrate) {
            triggerCelebrationVibration()
        }
    }

    fun triggerCelebrationVibration() {
        try {
            val vibrator = getVibrator() ?: return
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 100, 60, 100, 60, 300)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Celebration vibration failed", e)
        }
    }

    fun getAvailableRingtones(): List<MusicalSoundOption> = getAvailableMusicalSounds()

    fun saveCustomRingtone(inputStream: InputStream, fileName: String): Result<Pair<String, String>> {
        val dir = File(appContext.filesDir, "custom_ringtones").apply { if (!exists()) mkdirs() }
        val cleanBase = fileName.substringBeforeLast(".").ifBlank { "custom_audio" }
        val safeFileBase = cleanBase.replace(Regex("[^a-zA-Z0-9 _-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_', ' ')
            .take(40)
            .ifBlank { "custom_audio" }
        val ext = if (fileName.contains(".")) "." + fileName.substringAfterLast(".") else ".mp3"
        val tempFile = File(dir, "temp_${System.currentTimeMillis()}$ext")
        val finalFile = File(dir, "audio_${System.currentTimeMillis()}_$safeFileBase$ext")

        return try {
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
                output.flush()
            }
            if (tempFile.length() == 0L) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Audio stream is empty."))
            }
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
            val uriStr = Uri.fromFile(finalFile).toString()
            Result.success(Pair(uriStr, cleanBase))
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Log.e(TAG, "Failed to save custom ringtone: $fileName", e)
            Result.failure(e)
        }
    }

    /**
     * Deterministically stops and releases MediaPlayer, AudioTrack, fallback Ringtone,
     * and alarm vibration/audio focus.
     */
    fun stopPreview() {
        isLoopStopped = true
        playbackSeq.incrementAndGet()
        playbackJob?.cancel()
        playbackJob = null
        completionTimerJob?.cancel()
        completionTimerJob = null
        stopAudioTrack()
        stopFallbackRingtone()
        stopVibration()
        abandonAlarmAudioFocus()
        releaseMediaPlayerOnly()
    }

    /**
     * Deterministic resource cleanup. Releases MediaPlayer and AudioTrack, cancels child coroutines.
     */
    fun release() {
        stopAlarmLoop()
        playerScope.coroutineContext.cancelChildren()
    }

    data class MusicalSoundOption(
        val id: String,
        val name: String,
        val category: String, // "Calming & Relaxing", "Uplifting & Encouraging", "Focus & Peace", "Custom Imported"
        val description: String,
        val uriString: String,
        val isCustom: Boolean = false
    )

    data class BuiltInSoundDef(
        val key: String,
        val name: String,
        val category: String,
        val description: String,
        val frequencies: List<Float>,
        val durationsMs: List<Int>
    )

    private val builtInDefinitions = listOf(
        BuiltInSoundDef(
            key = "zen_bells",
            name = "Kyoto Temple Bronze Bell",
            category = "Calming & Relaxing",
            description = "Traditional resonant bronze bell with deep harmonic overtones. Grounds morning peace.",
            frequencies = listOf(220.00f, 329.63f, 440.00f, 659.25f, 880.00f),
            durationsMs = listOf(300, 300, 300, 400, 900)
        ),
        BuiltInSoundDef(
            key = "morning_harp",
            name = "Royal Windsor Harp Awakening",
            category = "Calming & Relaxing",
            description = "Majestic ascending acoustic harp arpeggio. Inspires smooth, radiant waking.",
            frequencies = listOf(261.63f, 329.63f, 392.00f, 523.25f, 659.25f, 783.99f),
            durationsMs = listOf(200, 200, 200, 200, 250, 800)
        ),
        BuiltInSoundDef(
            key = "serene_flow",
            name = "Tibetan Crystal Singing Bowl",
            category = "Calming & Relaxing",
            description = "Pure quartz crystal singing bowl resonance. Deeply restorative and tranquil.",
            frequencies = listOf(174.61f, 220.00f, 261.63f, 349.23f, 440.00f),
            durationsMs = listOf(350, 350, 350, 400, 1000)
        ),
        BuiltInSoundDef(
            key = "golden_sunrise",
            name = "Imperial Dawn Brass Fanfare",
            category = "Uplifting & Encouraging",
            description = "Warm radiant brass harmonics. Stimulates motivation and focused energy.",
            frequencies = listOf(293.66f, 369.99f, 440.00f, 587.33f, 739.99f, 880.00f),
            durationsMs = listOf(220, 220, 220, 250, 300, 850)
        ),
        BuiltInSoundDef(
            key = "celestial_dream",
            name = "Celestial Crystal Shimmer",
            category = "Focus & Peace",
            description = "Ethereal high-octave crystal chime sequence. Mindful, crystal-clear prompt.",
            frequencies = listOf(523.25f, 659.25f, 783.99f, 1046.50f, 1318.51f, 1567.98f),
            durationsMs = listOf(180, 180, 180, 200, 220, 900)
        ),
        BuiltInSoundDef(
            key = "triumph_fanfare",
            name = "Grand Victory Triumph Chime",
            category = "Uplifting & Encouraging",
            description = "Intensified celebratory brass fanfare. Reinforces streak completion.",
            frequencies = listOf(392.00f, 493.88f, 587.33f, 783.99f, 987.77f, 1174.66f),
            durationsMs = listOf(160, 160, 160, 200, 250, 950)
        ),
        BuiltInSoundDef(
            key = "deep_focus",
            name = "Deep Zen Meditation Gong",
            category = "Focus & Peace",
            description = "Deep reverberating monastic temple gong. Centers absolute attention and clarity.",
            frequencies = listOf(110.00f, 164.81f, 220.00f, 329.63f, 440.00f, 659.25f),
            durationsMs = listOf(400, 400, 400, 450, 500, 1200)
        ),
        BuiltInSoundDef(
            key = "vitality_pulse",
            name = "Emerald Forest Bamboo Flute",
            category = "Uplifting & Encouraging",
            description = "Traditional organic bamboo flute harmonics. Energizes daily follow-through.",
            frequencies = listOf(329.63f, 415.30f, 493.88f, 622.25f, 830.61f, 987.77f),
            durationsMs = listOf(200, 200, 200, 220, 250, 800)
        ),
        BuiltInSoundDef(
            key = "cyber_synth",
            name = "Cyberpunk Neon Synth Pulse",
            category = "Focus & Peace",
            description = "Futuristic retro-wave synth arpeggio. Electric, modern, and distinct.",
            frequencies = listOf(440.00f, 523.25f, 622.25f, 880.00f, 1046.50f),
            durationsMs = listOf(150, 150, 150, 200, 750)
        ),
        BuiltInSoundDef(
            key = "ocean_wave",
            name = "Oceanic Wave Chime",
            category = "Calming & Relaxing",
            description = "Rolling harmonic wave intervals. Soothing and deeply immersive.",
            frequencies = listOf(196.00f, 246.94f, 293.66f, 392.00f, 493.88f),
            durationsMs = listOf(300, 300, 350, 400, 900)
        ),
        BuiltInSoundDef(
            key = "starlight_box",
            name = "Velvet Music Box Lullaby",
            category = "Calming & Relaxing",
            description = "Delicate music box tines. Pure nostalgia and gentle waking.",
            frequencies = listOf(587.33f, 659.25f, 783.99f, 880.00f, 1046.50f),
            durationsMs = listOf(160, 160, 180, 200, 800)
        ),
        BuiltInSoundDef(
            key = "alpine_horn",
            name = "Alpine Morning Echo",
            category = "Uplifting & Encouraging",
            description = "Resonant mountain horn harmonic echo. Crisp, bold, and refreshing.",
            frequencies = listOf(349.23f, 440.00f, 523.25f, 698.46f, 880.00f),
            durationsMs = listOf(250, 250, 280, 350, 900)
        )
    )

    init {
        // Pre-cache WAV files in background using cancellable IO dispatcher only once
        if (!hasEnsuredBuiltins.get()) {
            playerScope.launch(Dispatchers.IO) {
                ensureMusicalWavFiles()
            }
        }
    }

    /**
     * Returns curated musical sounds + custom imported sounds metadata immediately
     * without blocking UI with synchronous WAV generation.
     */
    fun getAvailableMusicalSounds(): List<MusicalSoundOption> {
        val dir = File(appContext.filesDir, "musical_ringtones").apply { if (!exists()) mkdirs() }
        val list = mutableListOf<MusicalSoundOption>()

        for (def in builtInDefinitions) {
            val file = File(dir, "${def.key}.wav")
            list.add(
                MusicalSoundOption(
                    id = "musical_${def.key}",
                    name = def.name,
                    category = def.category,
                    description = def.description,
                    uriString = Uri.fromFile(file).toString()
                )
            )
        }

        // Load custom imported ringtones from internal storage
        val customDir = File(appContext.filesDir, "custom_ringtones")
        if (customDir.exists()) {
            customDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }?.sortedByDescending { it.lastModified() }?.forEach { file ->
                val base = file.nameWithoutExtension
                val cleanName = when {
                    base.startsWith("extracted_audio_") -> {
                        val raw = base.removePrefix("extracted_audio_")
                        val withoutTimestamp = if (raw.contains("_")) raw.substringAfter("_") else raw
                        "Voice: " + withoutTimestamp.ifBlank { "Extracted Voice" }
                    }
                    base.startsWith("audio_") -> {
                        val raw = base.removePrefix("audio_")
                        val withoutTimestamp = if (raw.contains("_")) raw.substringAfter("_") else raw
                        withoutTimestamp.ifBlank { "Custom Audio" }
                    }
                    else -> base
                }
                list.add(
                    MusicalSoundOption(
                        id = "custom_${file.name}",
                        name = cleanName,
                        category = "Custom Imported",
                        description = "Imported audio file stored on device",
                        uriString = Uri.fromFile(file).toString(),
                        isCustom = true
                    )
                )
            }
        }

        return list
    }

    /**
     * Synthesizes all missing built-in WAV files on the IO dispatcher with cancellation checks.
     * Uses memory buffering and atomic writes for maximum speed and zero main-thread interference.
     */
    suspend fun ensureMusicalWavFiles() = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "musical_ringtones").apply { if (!exists()) mkdirs() }
        var allExist = true
        for (def in builtInDefinitions) {
            val file = File(dir, "${def.key}.wav")
            if (!file.exists() || file.length() < 100) {
                allExist = false
                break
            }
        }
        if (allExist && hasEnsuredBuiltins.get()) {
            return@withContext
        }

        for (def in builtInDefinitions) {
            ensureActive()
            val file = File(dir, "${def.key}.wav")
            if (!file.exists() || file.length() < 100) {
                generateWavFileSafely(file, def.frequencies, def.durationsMs)
            }
        }
        hasEnsuredBuiltins.set(true)
    }

    /**
     * Checks if a target file belongs to built-in sounds and synthesizes it on-demand if not ready.
     */
    private suspend fun ensureFileReadyIfBuiltin(uri: Uri) = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext
        val fileName = File(path).name
        val key = fileName.substringBeforeLast(".wav")
        val def = builtInDefinitions.firstOrNull { it.key == key } ?: return@withContext
        val dir = File(appContext.filesDir, "musical_ringtones").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${def.key}.wav")
        if (!file.exists() || file.length() < 100) {
            generateWavFileSafely(file, def.frequencies, def.durationsMs)
        }
    }

    /**
     * Thread-safe, atomic WAV file generation. Synthesizes PCM into a memory buffer
     * and streams via BufferedOutputStream in one operation, renaming atomically
     * to eliminate partial reads, race conditions, or main-thread I/O stalls.
     */
    private suspend fun generateWavFileSafely(
        finalFile: File,
        frequencies: List<Float>,
        durationsMs: List<Int>
    ) = withContext(Dispatchers.IO) {
        val lock = fileGenerationLocks.getOrPut(finalFile.name) { Mutex() }
        lock.withLock {
            if (finalFile.exists() && finalFile.length() >= 100) {
                return@withLock
            }

            val parentDir = finalFile.parentFile ?: return@withLock
            if (!parentDir.exists()) parentDir.mkdirs()

            val tempFile = File(parentDir, "${finalFile.name}.tmp_${System.currentTimeMillis()}")
            try {
                val totalDurationMs = durationsMs.sum()
                val totalSamples = (SAMPLE_RATE * (totalDurationMs / 1000.0)).toInt().coerceAtLeast(0)
                val byteRate = SAMPLE_RATE * 2 // 16-bit mono = 2 bytes per sample
                val pcmData = synthesizeNotesToPcm(
                    frequencies = frequencies,
                    durationsMs = durationsMs,
                    checkCancellation = true,
                    coroutineContext = coroutineContext
                )
                val dataSize = pcmData.size.coerceAtLeast(totalSamples * 2).let { pcmData.size }

                // Write 44-byte WAV header and PCM data with buffered I/O
                BufferedOutputStream(FileOutputStream(tempFile), 64 * 1024).use { out ->
                    out.write("RIFF".toByteArray())
                    out.writeIntLe(36 + dataSize)
                    out.write("WAVE".toByteArray())
                    out.write("fmt ".toByteArray())
                    out.writeIntLe(16) // SubChunk1Size for PCM
                    out.writeShortLe(1) // AudioFormat 1 = PCM
                    out.writeShortLe(1) // NumChannels = 1 (Mono)
                    out.writeIntLe(SAMPLE_RATE)
                    out.writeIntLe(byteRate)
                    out.writeShortLe(2) // BlockAlign
                    out.writeShortLe(16) // BitsPerSample
                    out.write("data".toByteArray())
                    out.writeIntLe(dataSize)

                    out.write(pcmData, 0, pcmData.size)
                    out.flush()
                }

                if (tempFile.length() > 0) {
                    if (!tempFile.renameTo(finalFile)) {
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                if (e is CancellationException) throw e
                Log.e(TAG, "Error generating WAV file: ${finalFile.name}", e)
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }

    private fun OutputStream.writeIntLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun OutputStream.writeShortLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private fun safeSystemDefaultUri(fallbackFile: File): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Uri.fromFile(fallbackFile)
    }

    /**
     * Resolves raw strings, nicknames ("chime"), document URIs, and file URIs
     * into playable, verified URIs safely.
     */
    fun resolveUriSafely(uriString: String?): Uri {
        val dir = File(appContext.filesDir, "musical_ringtones")
        val defaultFile = File(dir, "zen_bells.wav")
        if (uriString.isNullOrBlank() || uriString == "chime" || uriString == "default") {
            return if (defaultFile.exists()) {
                Uri.fromFile(defaultFile)
            } else {
                safeSystemDefaultUri(defaultFile)
            }
        }

        return try {
            val parsed = Uri.parse(uriString)
            when (parsed.scheme) {
                "file" -> {
                    val file = File(parsed.path ?: "")
                    if (file.exists() && file.length() > 0) {
                        parsed
                    } else {
                        val key = file.nameWithoutExtension
                        val matched = File(dir, "$key.wav")
                        if (matched.exists()) Uri.fromFile(matched)
                        else safeSystemDefaultUri(defaultFile)
                    }
                }
                "content", "android.resource" -> parsed
                else -> {
                    val key = uriString.removeSuffix(".wav")
                    val matched = File(dir, "$key.wav")
                    if (matched.exists()) Uri.fromFile(matched)
                    else safeSystemDefaultUri(defaultFile)
                }
            }
        } catch (_: Throwable) {
            safeSystemDefaultUri(defaultFile)
        }
    }

    /**
     * Finds built-in sound definition by key or URI string.
     */
    fun findBuiltInSoundDef(uriString: String?): BuiltInSoundDef? {
        if (uriString.isNullOrBlank() || uriString == "chime" || uriString == "default") {
            return builtInDefinitions.firstOrNull { it.key == "zen_bells" }
        }
        val cleanKey = uriString
            .removePrefix("musical_")
            .removeSuffix(".wav")
            .substringAfterLast("/")
            .removeSuffix(".wav")
        return builtInDefinitions.firstOrNull { it.key == cleanKey }
    }

    /**
     * Synthesizes 16-bit PCM waveform in memory with smooth exponential decay and click-free envelopes.
     */
    fun generatePcmData(frequencies: List<Float>, durationsMs: List<Int>): ByteArray {
        return synthesizeNotesToPcm(frequencies, durationsMs, checkCancellation = false)
    }

}
