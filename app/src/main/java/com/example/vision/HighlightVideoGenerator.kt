package com.example.vision

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

private const val TAG = "HighlightVideoGenerator"

/**
 * Motor de highlights y multiplexado de audio/música para el modo Ball & Touch (Reaction Points).
 *
 * Permite:
 * 1. Generar vídeo de Highlights (12-16s) cortando y uniendo los momentos de combos y toques rápidos.
 * 2. Incorporar la pista rítmica de baloncesto (kantera_trap_beat.m4a) al contenedor MP4 sin re-codificación lenta.
 * 3. Compartir directamente en WhatsApp, Instagram Stories / Reels o selector nativo de Android.
 */
object HighlightVideoGenerator {

    private const val BEAT_ASSET_NAME = "audio/kantera_trap_beat.m4a"

    /**
     * Copia el asset de audio a la caché si aún no existe.
     */
    fun getOrExtractBeatFile(context: Context): File? {
        val cacheFile = File(context.cacheDir, "kantera_trap_beat.m4a")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile
        }
        return try {
            context.assets.open(BEAT_ASSET_NAME).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting beat audio asset: ${e.message}", e)
            null
        }
    }

    /**
     * Genera un vídeo compacto de mejores jugadas y combos (~12-16 segundos)
     * a partir de los eventos ocurridos durante la sesión.
     */
    suspend fun createHighlightVideo(
        context: Context,
        sourceVideoFile: File,
        moments: List<ReactionHighlightMoment>,
        withMusic: Boolean
    ): File = withContext(Dispatchers.IO) {
        if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
            Log.w(TAG, "Source video missing or empty, cannot generate highlights")
            return@withContext sourceVideoFile
        }

        val outputFile = File(
            context.cacheDir,
            "kantera_highlights_${System.currentTimeMillis()}.mp4"
        )

        try {
            // 1. Obtener duración total del vídeo fuente
            val totalDurationMs = getVideoDurationMs(sourceVideoFile).coerceAtLeast(5000L)

            // 2. Calcular intervalos de corte para los mejores momentos
            val intervals = calculateHighlightIntervals(moments, totalDurationMs)
            Log.i(TAG, "Highlight intervals count: ${intervals.size}, total durationMs: $totalDurationMs")

            // 3. Remuxing de vídeo con intervalos seleccionados
            val success = remuxSegments(
                context = context,
                sourceFile = sourceVideoFile,
                outputFile = outputFile,
                intervalsMs = intervals,
                withMusic = withMusic
            )

            if (success && outputFile.exists() && outputFile.length() > 0) {
                Log.i(TAG, "Highlight video generated: ${outputFile.length()} bytes")
                outputFile
            } else {
                Log.w(TAG, "Remuxing highlights failed, falling back to source video")
                sourceVideoFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create highlight video: ${e.message}", e)
            sourceVideoFile
        }
    }

    /**
     * Muxea la música en el vídeo completo si el usuario eligió Vídeo Completo con Música.
     */
    suspend fun createFullVideoWithMusic(
        context: Context,
        sourceVideoFile: File
    ): File = withContext(Dispatchers.IO) {
        if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
            return@withContext sourceVideoFile
        }
        val beatFile = getOrExtractBeatFile(context) ?: return@withContext sourceVideoFile
        val outputFile = File(
            context.cacheDir,
            "kantera_full_music_${System.currentTimeMillis()}.mp4"
        )

        try {
            val totalDurationMs = getVideoDurationMs(sourceVideoFile).coerceAtLeast(3000L)
            val fullInterval = listOf(Pair(0L, totalDurationMs))

            val success = remuxSegments(
                context = context,
                sourceFile = sourceVideoFile,
                outputFile = outputFile,
                intervalsMs = fullInterval,
                withMusic = true
            )

            if (success && outputFile.exists() && outputFile.length() > 0) {
                outputFile
            } else {
                sourceVideoFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed adding music to full video: ${e.message}", e)
            sourceVideoFile
        }
    }

    /**
     * Selecciona y fusiona intervalos de tiempo (en ms) alrededor de los mejores combos y aciertos.
     */
    private fun calculateHighlightIntervals(
        moments: List<ReactionHighlightMoment>,
        totalDurationMs: Long
    ): List<Pair<Long, Long>> {
        if (moments.isEmpty()) {
            // Fallback si no hubo toques registrados: tomar ventana de 12 segundos centrales
            val start = max(0L, (totalDurationMs - 12000L) / 2)
            val end = min(totalDurationMs, start + 12000L)
            return listOf(Pair(start, end))
        }

        // Priorizar momentos: Combos primero, luego momentos con mayor puntuación
        val sortedMoments = moments.sortedWith(
            compareByDescending<ReactionHighlightMoment> { it.isCombo }
                .thenByDescending { it.score }
        )

        // Tomar hasta 5 momentos destacados
        val topMoments = sortedMoments.take(5).sortedBy { it.timestampMs }

        // Crear ventanas de ~2.2 segundos por momento (-1.4s antes del hit, +0.8s después)
        val rawIntervals = topMoments.map { m ->
            val start = max(0L, m.timestampMs - 1400L)
            val end = min(totalDurationMs, m.timestampMs + 800L)
            Pair(start, end)
        }

        // Fusionar intervalos superpuestos o muy cercanos (< 500ms de diferencia)
        val merged = mutableListOf<Pair<Long, Long>>()
        for (interval in rawIntervals) {
            if (merged.isEmpty()) {
                merged.add(interval)
            } else {
                val last = merged.last()
                if (interval.first <= last.second + 500L) {
                    // Combinar
                    merged[merged.size - 1] = Pair(last.first, max(last.second, interval.second))
                } else {
                    merged.add(interval)
                }
            }
        }

        // Si la suma de intervalos es muy breve (<8s), expandir para lograr un reel dinámico de 8-15s
        val totalHighlightsMs = merged.sumOf { it.second - it.first }
        if (totalHighlightsMs < 8000L && totalDurationMs > 8000L) {
            val neededExtra = (8000L - totalHighlightsMs) / (merged.size * 2)
            return merged.map { interval ->
                Pair(
                    max(0L, interval.first - neededExtra),
                    min(totalDurationMs, interval.second + neededExtra)
                )
            }
        }

        return merged
    }

    private fun getVideoDurationMs(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Remuxing ultrarrápido con MediaExtractor y MediaMuxer.
     * Intercala vídeo y audio AAC fotograma a fotograma en orden cronológico estricto,
     * garantizando reproducción instantánea en WhatsApp, Instagram, ExoPlayer y navegadores web
     * sin cuelgues ni tiempos de carga infinitos.
     */
    private fun remuxSegments(
        context: Context,
        sourceFile: File,
        outputFile: File,
        intervalsMs: List<Pair<Long, Long>>,
        withMusic: Boolean
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            videoExtractor = MediaExtractor().apply {
                setDataSource(sourceFile.absolutePath)
            }

            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndex < 0 || videoFormat == null) {
                Log.e(TAG, "No video track found in source file")
                return false
            }

            videoExtractor.selectTrack(videoTrackIndex)

            // Configurar pista de Audio si se solicita música con ritmo
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            if (withMusic) {
                val beatFile = getOrExtractBeatFile(context)
                if (beatFile != null && beatFile.exists() && beatFile.length() > 0) {
                    try {
                        val aExt = MediaExtractor().apply {
                            setDataSource(beatFile.absolutePath)
                        }
                        for (i in 0 until aExt.trackCount) {
                            val format = aExt.getTrackFormat(i)
                            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                            if (mime.startsWith("audio/")) {
                                audioTrackIndex = i
                                audioFormat = format
                                audioExtractor = aExt
                                break
                            }
                        }
                        if (audioExtractor != null && audioTrackIndex >= 0) {
                            audioExtractor.selectTrack(audioTrackIndex)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed preparing audio extractor: ${e.message}")
                    }
                }
            }

            // Inicializar contenedor MP4 MediaMuxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxVideoTrack = muxer.addTrack(videoFormat)
            val muxAudioTrack = if (audioExtractor != null && audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else {
                -1
            }

            muxer.start()

            val videoBufferSize = 1024 * 1024 // 1 MB
            val videoBuffer = ByteBuffer.allocateDirect(videoBufferSize)
            val videoInfo = MediaCodec.BufferInfo()

            val audioBufferSize = 256 * 1024 // 256 KB
            val audioBuffer = ByteBuffer.allocateDirect(audioBufferSize)
            val audioInfo = MediaCodec.BufferInfo()

            var currentVideoPtsUs = 0L
            var currentAudioPtsUs = 0L
            val videoFrameDurationUs = 33_333L // ~30 fps

            val audioSampleRate = if (audioFormat != null && audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            } else {
                44100
            }
            val audioFrameDurationUs = (1024L * 1_000_000L) / audioSampleRate

            // Escribe paquetes AAC completos e intercalados hasta igualar el timestamp de vídeo
            fun writeAudioUpTo(targetPtsUs: Long) {
                val aExt = audioExtractor ?: return
                if (muxAudioTrack < 0) return

                while (currentAudioPtsUs <= targetPtsUs) {
                    audioBuffer.clear()
                    var sampleSize = aExt.readSampleData(audioBuffer, 0)
                    if (sampleSize <= 0) {
                        // Loop continuo de la base musical si la sesión supera la duración del audio
                        aExt.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        sampleSize = aExt.readSampleData(audioBuffer, 0)
                        if (sampleSize <= 0) break
                    }

                    audioBuffer.position(0)
                    audioBuffer.limit(sampleSize)
                    audioInfo.offset = 0
                    audioInfo.size = sampleSize
                    audioInfo.presentationTimeUs = currentAudioPtsUs
                    audioInfo.flags = aExt.sampleFlags

                    muxer.writeSampleData(muxAudioTrack, audioBuffer, audioInfo)
                    currentAudioPtsUs += audioFrameDurationUs

                    if (!aExt.advance()) {
                        aExt.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    }
                }
            }

            // Remuxing e intercalado estricto fotograma a fotograma
            for (interval in intervalsMs) {
                val startUs = interval.first * 1000L
                val endUs = interval.second * 1000L

                videoExtractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                while (true) {
                    val sampleTimeUs = videoExtractor.sampleTime
                    if (sampleTimeUs < 0 || sampleTimeUs > endUs) {
                        break
                    }

                    videoBuffer.clear()
                    val sampleSize = videoExtractor.readSampleData(videoBuffer, 0)
                    if (sampleSize < 0) {
                        break
                    }

                    videoBuffer.position(0)
                    videoBuffer.limit(sampleSize)
                    videoInfo.offset = 0
                    videoInfo.size = sampleSize
                    videoInfo.presentationTimeUs = currentVideoPtsUs
                    var flags = videoExtractor.sampleFlags
                    if (currentVideoPtsUs == 0L) {
                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    videoInfo.flags = flags

                    // Escribir audio intercalado antes o a la par del vídeo
                    writeAudioUpTo(currentVideoPtsUs)

                    // Escribir frame de vídeo
                    muxer.writeSampleData(muxVideoTrack, videoBuffer, videoInfo)
                    currentVideoPtsUs += videoFrameDurationUs

                    if (!videoExtractor.advance()) {
                        break
                    }
                }
            }

            // Completar los últimos paquetes de audio hasta igualar la duración total del vídeo
            writeAudioUpTo(currentVideoPtsUs)

            muxer.stop()
            Log.i(TAG, "Remuxing completed successfully: ${outputFile.length()} bytes, durationUs=$currentVideoPtsUs")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during remuxing: ${e.message}", e)
            return false
        } finally {
            try {
                videoExtractor?.release()
            } catch (_: Exception) {}
            try {
                audioExtractor?.release()
            } catch (_: Exception) {}
            try {
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Comparte directamente a WhatsApp mediante ACTION_SEND con FileProvider.
     */
    fun shareToWhatsApp(context: Context, videoFile: File, score: Int) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            val text = "🏀 ¡Mira mi sesión en Ball & Touch de Kantera AI! He logrado $score puntos 🔥 #KanteraAI"

            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.whatsapp")
            }

            val pm = context.packageManager
            if (waIntent.resolveActivity(pm) != null) {
                context.startActivity(waIntent)
            } else {
                // Probar WhatsApp Business
                val waBusinessIntent = Intent(waIntent).apply { 
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (waBusinessIntent.resolveActivity(pm) != null) {
                    context.startActivity(waBusinessIntent)
                } else {
                    Toast.makeText(context, "WhatsApp no encontrado, abriendo selector...", Toast.LENGTH_SHORT).show()
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, text)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Compartir en WhatsApp u otra aplicación"
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing to WhatsApp: ${e.message}", e)
            Toast.makeText(context, "Error al compartir en WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Comparte directamente a Instagram (Stories o Feed/Reels).
     */
    fun shareToInstagram(context: Context, videoFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )

            // 1. Intent para Instagram Stories
            val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("content_url", "https://kantera.app")
            }

            val pm = context.packageManager
            if (storiesIntent.resolveActivity(pm) != null) {
                context.startActivity(storiesIntent)
                return
            }

            // 2. Intent para Instagram Feed / Reels
            val igIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (igIntent.resolveActivity(pm) != null) {
                context.startActivity(igIntent)
                return
            }

            // 3. Fallback al selector de sistema
            Toast.makeText(context, "Instagram no encontrado, abriendo opciones...", Toast.LENGTH_SHORT).show()
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "🏀 Mi sesión de Ball & Touch en Kantera AI #KanteraAI")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Compartir en Instagram u otra aplicación"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing to Instagram: ${e.message}", e)
            Toast.makeText(context, "Error al compartir en Instagram: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Selector general para compartir con cualquier otra aplicación instalada.
     */
    fun shareGeneral(context: Context, videoFile: File, title: String = "Compartir vídeo Kantera") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "🏀 Mira mi entrenamiento en Kantera AI #KanteraAI")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening general share: ${e.message}", e)
        }
    }
}
