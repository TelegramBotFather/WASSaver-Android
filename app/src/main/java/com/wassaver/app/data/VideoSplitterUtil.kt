package com.wassaver.app.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Utility to split a video into segments of a given duration (default 90 seconds)
 * using Android's built-in MediaExtractor + MediaMuxer APIs.
 * No external FFmpeg dependency required.
 */
object VideoSplitterUtil {

    private const val TAG = "VideoSplitterUtil"
    private const val DEFAULT_SEGMENT_DURATION_MS = 90_000L // 90 seconds
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB

    data class SplitResult(
        val segments: List<File>,
        val totalDurationMs: Long,
        val segmentDurationMs: Long
    )

    /**
     * Get the duration of a video in milliseconds
     */
    suspend fun getVideoDurationMs(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return@withContext 0L

            var maxDuration = 0L
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    val duration = format.getLong(MediaFormat.KEY_DURATION)
                    if (duration > maxDuration) maxDuration = duration
                }
            }
            maxDuration / 1000 // Convert microseconds to milliseconds
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video duration", e)
            0L
        } finally {
            extractor.release()
        }
    }

    /**
     * Split a video into segments of the specified duration.
     * Returns a list of File objects pointing to the split segments stored in the cache directory.
     *
     * @param context Android context
     * @param uri URI of the source video
     * @param segmentDurationMs Duration of each segment in milliseconds (default 90s)
     * @param onProgress Callback with progress (0.0f to 1.0f)
     */
    suspend fun splitVideo(
        context: Context,
        uri: Uri,
        segmentDurationMs: Long = DEFAULT_SEGMENT_DURATION_MS,
        onProgress: (Float) -> Unit = {}
    ): SplitResult = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "split_videos").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return@withContext SplitResult(emptyList(), 0L, segmentDurationMs)

            // Find video and audio tracks
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null
            var totalDurationUs = 0L

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                    videoFormat = format
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        val d = format.getLong(MediaFormat.KEY_DURATION)
                        if (d > totalDurationUs) totalDurationUs = d
                    }
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                    audioFormat = format
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        val d = format.getLong(MediaFormat.KEY_DURATION)
                        if (d > totalDurationUs) totalDurationUs = d
                    }
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                Log.e(TAG, "No video track found")
                return@withContext SplitResult(emptyList(), 0L, segmentDurationMs)
            }

            val totalDurationMs = totalDurationUs / 1000
            val segmentDurationUs = segmentDurationMs * 1000
            val segmentCount = ((totalDurationUs + segmentDurationUs - 1) / segmentDurationUs).toInt()

            Log.d(TAG, "Video duration: ${totalDurationMs}ms, Segments: $segmentCount")

            val segments = mutableListOf<File>()

            for (segIdx in 0 until segmentCount) {
                val startTimeUs = segIdx.toLong() * segmentDurationUs
                val endTimeUs = minOf((segIdx + 1).toLong() * segmentDurationUs, totalDurationUs)

                val outputFile = File(outputDir, "part_${segIdx + 1}.mp4")
                
                try {
                    splitSegment(
                        context = context,
                        uri = uri,
                        outputFile = outputFile,
                        startTimeUs = startTimeUs,
                        endTimeUs = endTimeUs,
                        videoTrackIndex = videoTrackIndex,
                        audioTrackIndex = audioTrackIndex,
                        videoFormat = videoFormat,
                        audioFormat = audioFormat
                    )
                    
                    if (outputFile.exists() && outputFile.length() > 0) {
                        segments.add(outputFile)
                        Log.d(TAG, "Created segment ${segIdx + 1}: ${outputFile.length()} bytes")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating segment ${segIdx + 1}", e)
                }

                onProgress((segIdx + 1).toFloat() / segmentCount)
            }

            SplitResult(segments, totalDurationMs, segmentDurationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error splitting video", e)
            SplitResult(emptyList(), 0L, segmentDurationMs)
        } finally {
            extractor.release()
        }
    }

    private fun splitSegment(
        context: Context,
        uri: Uri,
        outputFile: File,
        startTimeUs: Long,
        endTimeUs: Long,
        videoTrackIndex: Int,
        audioTrackIndex: Int,
        videoFormat: MediaFormat,
        audioFormat: MediaFormat?
    ) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Add tracks to muxer
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = if (audioTrackIndex >= 0 && audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else {
                -1
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            // Write video track
            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            while (true) {
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0 || sampleTime >= endTimeUs) break

                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTime - startTimeUs
                bufferInfo.flags = extractor.sampleFlags

                if (sampleTime >= startTimeUs) {
                    muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                }

                extractor.advance()
            }

            // Write audio track
            if (audioTrackIndex >= 0 && muxerAudioTrack >= 0) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                while (true) {
                    val sampleTime = extractor.sampleTime
                    if (sampleTime < 0 || sampleTime >= endTimeUs) break

                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTime - startTimeUs
                    bufferInfo.flags = extractor.sampleFlags

                    if (sampleTime >= startTimeUs) {
                        muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    }

                    extractor.advance()
                }
            }

        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping muxer", e)
            }
            extractor.release()
        }
    }

    /**
     * Clean up split video cache
     */
    fun cleanupCache(context: Context) {
        File(context.cacheDir, "split_videos").let {
            if (it.exists()) it.deleteRecursively()
        }
    }

    /**
     * Format duration in ms to a human-readable string like "2m 30s"
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    /**
     * Format file size to human-readable string
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
