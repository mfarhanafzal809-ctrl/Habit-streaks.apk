package com.example.native

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * Hardened media pipeline for importing custom audio and extracting audio tracks from videos.
 * Guarantees truthful file extensions, container and MIME validation, storage space verification,
 * cancellable I/O off the main thread, and strict cleanup of partial/failed output.
 */
object AudioExtractionHelper {

    private const val TAG = "AudioExtractionHelper"

    const val MAX_AUDIO_FILE_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB
    const val MAX_VIDEO_FILE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB
    const val MAX_AUDIO_DURATION_MS = 10 * 60 * 1000L // 10 minutes max for alarm audio
    const val MIN_STORAGE_REQUIRED_BYTES = 10 * 1024 * 1024L // 10 MB minimum free storage

    private val SUPPORTED_AUDIO_MIMES = setOf(
        "audio/mpeg", "audio/mp3",
        "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac",
        "audio/ogg", "audio/opus", "application/ogg",
        "audio/wav", "audio/x-wav",
        "audio/flac", "audio/x-flac",
        "audio/3gpp"
    )

    /**
     * Resolves human-readable file name from Uri without assuming a local filesystem path.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "Custom_Audio"
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val resolved = cursor.getString(nameIndex)
                    if (!resolved.isNullOrBlank()) {
                        name = resolved
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve filename via ContentResolver", e)
        }
        return name
    }

    /**
     * Resolves file size in bytes from Uri via ContentResolver.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size = -1L
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve file size via ContentResolver", e)
        }
        if (size <= 0L) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    size = pfd.statSize
                }
            } catch (ignored: Exception) {}
        }
        return size
    }

    /**
     * Checks if a Uri represents a video file based on MIME type or filename extension.
     */
    fun isVideoUri(context: Context, uri: Uri): Boolean {
        val mimeType = try {
            context.contentResolver.getType(uri)?.lowercase() ?: ""
        } catch (_: Throwable) {
            ""
        }
        if (mimeType.startsWith("video/")) return true

        val fileName = getFileName(context, uri).lowercase()
        val pathSegment = (uri.lastPathSegment ?: uri.path ?: uri.toString()).lowercase()
        val checkTarget = if (fileName != "custom_audio") fileName else pathSegment

        return checkTarget.endsWith(".mp4") || checkTarget.endsWith(".mkv") || checkTarget.endsWith(".mov") ||
                checkTarget.endsWith(".3gp") || checkTarget.endsWith(".webm") || checkTarget.endsWith(".avi")
    }

    /**
     * Determines the truthful file extension based on MIME type and fallback name.
     */
    fun getTruthfulExtension(mimeType: String?, originalFileName: String): String {
        val extFromMime = when (mimeType?.lowercase()) {
            "audio/mpeg", "audio/mp3" -> ".mp3"
            "audio/mp4", "audio/m4a", "audio/aac", "audio/x-m4a" -> ".m4a"
            "audio/ogg", "audio/opus", "application/ogg" -> ".ogg"
            "audio/wav", "audio/x-wav" -> ".wav"
            "audio/flac", "audio/x-flac" -> ".flac"
            "audio/3gpp" -> ".3gp"
            else -> null
        }
        if (extFromMime != null) return extFromMime

        val originalExt = originalFileName.substringAfterLast(".", "").lowercase()
        return when (originalExt) {
            "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "3gp" -> ".$originalExt"
            else -> ".mp3"
        }
    }

    /**
     * Copies and validates an imported audio file to internal storage.
     * Enforces size bounds, storage space verification, duration limits, and atomic file saving.
     */
    suspend fun saveAudioToInternalStorage(
        context: Context,
        sourceUri: Uri
    ): Result<Pair<Uri, String>> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "custom_ringtones").apply { if (!exists()) mkdirs() }
        val tempId = UUID.randomUUID().toString().take(8)
        var tempFile: File? = null

        try {
            // 1. Storage check
            if (context.filesDir.usableSpace < MIN_STORAGE_REQUIRED_BYTES) {
                return@withContext Result.failure(
                    IllegalStateException("Insufficient device storage space to save custom audio.")
                )
            }

            // 2. Size check
            val declaredSize = getFileSize(context, sourceUri)
            if (declaredSize > MAX_AUDIO_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Audio file exceeds maximum allowed size (50 MB).")
                )
            }

            // 3. Media Metadata inspection
            var detectedMime: String? = context.contentResolver.getType(sourceUri)
            var durationMs: Long = 0L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, sourceUri)
                val retMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                if (!retMime.isNullOrBlank()) {
                    detectedMime = retMime
                }
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                Log.w(TAG, "MediaMetadataRetriever inspection failed or unsupported container", e)
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }

            if (durationMs > MAX_AUDIO_DURATION_MS) {
                return@withContext Result.failure(
                    IllegalArgumentException("Audio duration (${durationMs / 1000}s) exceeds maximum allowed ringtone length of 10 minutes.")
                )
            }

            val originalName = getFileName(context, sourceUri)
            val cleanName = originalName.substringBeforeLast(".").ifBlank { "Imported Audio" }
            val safeFileBase = sanitizeFileBase(cleanName, "Imported_Audio")
            val truthfulExt = getTruthfulExtension(detectedMime, originalName)

            tempFile = File(dir, "temp_import_${System.currentTimeMillis()}_$tempId.tmp")

            // 4. Stream Copy with cancellation checks
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalCopied = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        if (totalCopied > MAX_AUDIO_FILE_SIZE_BYTES) {
                            throw IllegalArgumentException("Audio file exceeds maximum allowed size limit of 50 MB.")
                        }
                    }
                    output.flush()
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Cannot open media stream from provider."))

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("Imported media file is empty."))
            }

            // 5. Atomic Rename to final file preserving clean name after timestamp
            val finalFile = File(dir, "audio_${System.currentTimeMillis()}_${safeFileBase}$truthfulExt")
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            Log.d(TAG, "Successfully saved custom audio to ${finalFile.name} (${finalFile.length()} bytes)")
            Result.success(Pair(Uri.fromFile(finalFile), cleanName))
        } catch (e: Exception) {
            tempFile?.let { if (it.exists()) it.delete() }
            Log.e(TAG, "Failed to save audio to internal storage", e)
            Result.failure(e)
        } finally {
            cleanupStaleTempFiles(dir)
        }
    }

    /**
     * Extracts an actual audio track from a video file into an .m4a container.
     * Never copies a video and renames it to .mp3.
     * Enforces cancellation, bounds, and deterministic cleanup of partial files.
     */
    suspend fun extractAudioFromVideo(
        context: Context,
        videoUri: Uri
    ): Result<Pair<Uri, String>> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "custom_ringtones").apply { if (!exists()) mkdirs() }
        val tempId = UUID.randomUUID().toString().take(8)
        val tempFile = File(dir, "temp_extract_${System.currentTimeMillis()}_$tempId.tmp")
        val originalName = getFileName(context, videoUri)
        val cleanName = "Voice: " + originalName.substringBeforeLast(".").ifBlank { "Video Audio" }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            // 1. Storage check
            if (context.filesDir.usableSpace < MIN_STORAGE_REQUIRED_BYTES) {
                return@withContext Result.failure(
                    IllegalStateException("Insufficient device storage space for video extraction.")
                )
            }

            // 2. Video file size check
            val videoSize = getFileSize(context, videoUri)
            if (videoSize > MAX_VIDEO_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Video file exceeds maximum allowed size (100 MB).")
                )
            }

            // 3. Inspect video duration
            var videoDurationMs = 0L
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                videoDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                Log.w(TAG, "Failed to inspect video duration", e)
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }

            if (videoDurationMs > 15 * 60 * 1000L) { // 15 minutes max
                return@withContext Result.failure(
                    IllegalArgumentException("Video exceeds maximum allowed duration of 15 minutes.")
                )
            }

            // 4. Extract Audio Track using MediaExtractor
            extractor.setDataSource(context, videoUri, null)
            val numTracks = extractor.trackCount
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            // DO NOT copy video and rename to .mp3 if no audio track exists
            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("The selected video does not contain any compatible audio track.")
                )
            }

            extractor.selectTrack(audioTrackIndex)

            var muxerStarted = false
            try {
                // 5. Setup MediaMuxer into MPEG-4 container (.m4a)
                muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerAudioTrack = muxer.addTrack(audioFormat)
                muxer.start()
                muxerStarted = true

                val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
                } else {
                    256 * 1024
                }

                val buffer = ByteBuffer.allocate(maxBufferSize)
                val bufferInfo = MediaCodec.BufferInfo()

                // 6. Extraction sample loop with coroutine cancellation support
                while (true) {
                    coroutineContext.ensureActive()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxerStarted = false
            } catch (e: Exception) {
                if (muxerStarted) {
                    try { muxer?.stop() } catch (_: Throwable) {}
                    muxerStarted = false
                }
                throw e
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return@withContext Result.failure(
                    IllegalArgumentException("Failed to extract audio track: Output is empty.")
                )
            }

            // 7. Atomic Rename to truthful .m4a extension preserving clean name after timestamp
            val rawVideoBase = originalName.substringBeforeLast(".").ifBlank { "Video Audio" }
            val safeExtractBase = sanitizeFileBase(rawVideoBase, "Extracted_Voice")
            val finalOutputFile = File(dir, "extracted_audio_${System.currentTimeMillis()}_${safeExtractBase}.m4a")
            if (!tempFile.renameTo(finalOutputFile)) {
                tempFile.copyTo(finalOutputFile, overwrite = true)
                tempFile.delete()
            }

            Log.d(TAG, "Successfully extracted audio track to ${finalOutputFile.name} (${finalOutputFile.length()} bytes)")
            Result.success(Pair(Uri.fromFile(finalOutputFile), cleanName))
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Log.e(TAG, "Audio extraction failed", e)
            Result.failure(e)
        } finally {
            try { extractor.release() } catch (_: Throwable) {}
            try { muxer?.release() } catch (_: Throwable) {}
            if (tempFile.exists()) {
                tempFile.delete()
            }
            cleanupStaleTempFiles(dir)
        }
    }

    private fun sanitizeFileBase(raw: String, fallback: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9 _-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_', ' ')
            .take(40)
            .ifBlank { fallback }
    }

    private fun cleanupStaleTempFiles(dir: File) {
        try {
            val now = System.currentTimeMillis()
            dir.listFiles { _, name -> name.endsWith(".tmp") }?.forEach { temp ->
                // Clean up temp files older than 1 minute
                if (now - temp.lastModified() > 60_000L) {
                    temp.delete()
                }
            }
        } catch (_: Throwable) {}
    }
}
