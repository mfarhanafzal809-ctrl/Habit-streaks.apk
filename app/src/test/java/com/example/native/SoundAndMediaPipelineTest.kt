package com.example.native

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SoundAndMediaPipelineTest {

    private lateinit var context: Context
    private lateinit var soundPlayer: SoundPlayer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        soundPlayer = SoundPlayer(context)
    }

    @Test
    fun testBuiltinSounds_returnMetadataImmediately_withoutBlocking() {
        val sounds = soundPlayer.getAvailableMusicalSounds()
        assertEquals(12, sounds.filter { !it.isCustom }.size)

        val first = sounds.first()
        assertEquals("Kyoto Temple Bronze Bell", first.name)
        assertEquals("Calming & Relaxing", first.category)
        assertTrue(first.uriString.endsWith("zen_bells.wav"))
    }

    @Test
    fun testBuiltinWav_generationOffMainThread_createsValidWavFiles() = runBlocking {
        soundPlayer.ensureMusicalWavFiles()

        val dir = File(context.filesDir, "musical_ringtones")
        assertTrue(dir.exists())

        val zenFile = File(dir, "zen_bells.wav")
        assertTrue(zenFile.exists())
        assertTrue(zenFile.length() > 44) // WAV header is 44 bytes

        val header = ByteArray(4)
        zenFile.inputStream().use { it.read(header) }
        assertEquals("RIFF", String(header))
    }

    @Test
    fun testMissingBuiltInFiles_resolvesAndSynthesizesOrFallsBackSafely() {
        // Resolve a non-existent built-in key or missing file safely
        val uri = soundPlayer.resolveUriSafely("missing_sound_key")
        assertNotNull(uri)
        assertTrue(uri.toString().isNotEmpty())

        // Play preview with null or missing sound key without crashing
        soundPlayer.playPreview("file:///non_existent_path.wav", vibrate = false)
        soundPlayer.stopPreview()
    }

    @Test
    fun testMalformedMedia_handlesGracefullyWithoutCrashing() {
        // Broken / invalid URI formats
        val invalidUri = soundPlayer.resolveUriSafely(":::not a valid uri???")
        assertNotNull(invalidUri)

        // Nicknames like "chime" or "default"
        val chimeUri = soundPlayer.resolveUriSafely("chime")
        assertNotNull(chimeUri)

        val defaultUri = soundPlayer.resolveUriSafely("default")
        assertNotNull(defaultUri)

        soundPlayer.playPreview("invalid_garbage_uri_format", vibrate = false)
        soundPlayer.stopPreview()
    }

    @Test
    fun testDeterministicResourceRelease_cleansUpSafely() {
        soundPlayer.playPreview(null, vibrate = false)
        soundPlayer.release()

        assertFalse(soundPlayer.isPlaying())
        // Repeated release calls must be idempotent and safe
        soundPlayer.release()
    }

    @Test
    fun testSaveCustomRingtone_validAudioStream_persistsWithCleanName() {
        val dummyPcm = ByteArray(1024) { (it % 127).toByte() }
        val stream = ByteArrayInputStream(dummyPcm)

        val result = soundPlayer.saveCustomRingtone(stream, "my_custom_alarm.mp3")
        assertTrue(result.isSuccess)

        val (uriStr, cleanName) = result.getOrThrow()
        assertEquals("my_custom_alarm", cleanName)
        assertTrue(uriStr.startsWith("file://"))

        val file = File(Uri.parse(uriStr).path ?: "")
        assertTrue(file.exists())
        assertEquals(1024L, file.length())

        // Ensure custom sound is reflected in sound list
        val sounds = soundPlayer.getAvailableMusicalSounds()
        val customSound = sounds.find { it.name == "my_custom_alarm" }
        assertNotNull(customSound)
        assertTrue(customSound!!.isCustom)
    }

    @Test
    fun testSaveCustomRingtone_emptyStreamFailsSafely() {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        val result = soundPlayer.saveCustomRingtone(emptyStream, "empty.mp3")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun testTruthfulExtensionMapping() {
        assertEquals(".mp3", AudioExtractionHelper.getTruthfulExtension("audio/mpeg", "test.audio"))
        assertEquals(".mp3", AudioExtractionHelper.getTruthfulExtension("audio/mp3", "test.audio"))
        assertEquals(".m4a", AudioExtractionHelper.getTruthfulExtension("audio/mp4", "test.audio"))
        assertEquals(".m4a", AudioExtractionHelper.getTruthfulExtension("audio/x-m4a", "test.audio"))
        assertEquals(".m4a", AudioExtractionHelper.getTruthfulExtension("audio/aac", "test.audio"))
        assertEquals(".ogg", AudioExtractionHelper.getTruthfulExtension("audio/ogg", "test.audio"))
        assertEquals(".wav", AudioExtractionHelper.getTruthfulExtension("audio/x-wav", "test.audio"))
        assertEquals(".flac", AudioExtractionHelper.getTruthfulExtension("audio/flac", "test.audio"))
        assertEquals(".3gp", AudioExtractionHelper.getTruthfulExtension("audio/3gpp", "test.audio"))
    }

    @Test
    fun testIsVideoUriDetection() {
        val videoUriMp4 = Uri.parse("content://media/external/video/media/123.mp4")
        val audioUriMp3 = Uri.parse("content://media/external/audio/media/456.mp3")

        assertTrue(AudioExtractionHelper.isVideoUri(context, videoUriMp4))
        assertFalse(AudioExtractionHelper.isVideoUri(context, audioUriMp3))
    }

    @Test
    fun testRepeatedPreview_swapsAudioWithoutLeakingOrCrashing() {
        soundPlayer.playPreview("file:///dummy_one.wav", vibrate = false)
        soundPlayer.playPreview("file:///dummy_two.wav", vibrate = false)
        soundPlayer.stopPreview()
        soundPlayer.playPreview(null, vibrate = false)
        soundPlayer.release()
    }

    @Test
    fun testPickerCancellationAndRecreation_releasesPlayerCleanly() {
        val pickerPlayer1 = SoundPlayer(context)
        pickerPlayer1.playPreview(null, vibrate = false)
        pickerPlayer1.release()

        val pickerPlayer2 = SoundPlayer(context)
        pickerPlayer2.playPreview(null, vibrate = false)
        pickerPlayer2.stopPreview()
        pickerPlayer2.release()
    }

    @Test
    fun testCelebrationFanfare_playsAsynchronouslyWithoutThrowing() {
        soundPlayer.playCelebrationFanfare()
        soundPlayer.stopPreview()
        soundPlayer.release()
    }

    @Test
    fun testSizeBoundsAndStorageConstants() {
        assertTrue(AudioExtractionHelper.MAX_AUDIO_FILE_SIZE_BYTES >= 50 * 1024 * 1024L)
        assertTrue(AudioExtractionHelper.MAX_VIDEO_FILE_SIZE_BYTES >= 100 * 1024 * 1024L)
        assertTrue(AudioExtractionHelper.MAX_AUDIO_DURATION_MS >= 10 * 60 * 1000L)
        assertTrue(AudioExtractionHelper.MIN_STORAGE_REQUIRED_BYTES >= 10 * 1024 * 1024L)
    }

    @Test
    fun testPcmSynthesis_producesSmoothTaperedEdgesAndHandlesRests() {
        val pcm = soundPlayer.generatePcmData(
            frequencies = listOf(440f, 0f, 523.25f),
            durationsMs = listOf(80, 40, 100)
        )
        assertTrue(pcm.size > 1000)
        assertEquals(0, pcm.size % 2)

        // First sample must start at 0 due to 8ms attack ramp (no initial click)
        val firstSample = ((pcm[1].toInt() shl 8) or (pcm[0].toInt() and 0xFF)).toShort()
        assertEquals(0, firstSample.toInt())

        // Last sample must end near 0 due to 15ms release ramp (no loop boundary pop)
        val lastIdx = pcm.size - 2
        val lastSample = ((pcm[lastIdx + 1].toInt() shl 8) or (pcm[lastIdx].toInt() and 0xFF)).toShort()
        assertTrue("Expected final PCM sample near 0, was $lastSample", kotlin.math.abs(lastSample.toInt()) <= 25)
    }

    @Test
    fun testSaveCustomRingtone_sanitizesUnsafeCharactersInFilename() {
        val dummyPcm = ByteArray(512) { 42 }
        val result = soundPlayer.saveCustomRingtone(
            ByteArrayInputStream(dummyPcm),
            "My: Weird/Custom?Alarm!.mp3"
        )
        assertTrue(result.isSuccess)
        val (uriStr, _) = result.getOrThrow()
        val savedFile = File(Uri.parse(uriStr).path ?: "")
        assertTrue(savedFile.exists())
        assertFalse(savedFile.name.contains(":"))
        assertFalse(savedFile.name.contains("?"))
        assertFalse(savedFile.name.contains("!"))
    }
}
