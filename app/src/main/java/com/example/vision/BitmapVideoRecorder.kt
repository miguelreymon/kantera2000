package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BitmapVideoRecorder"

/**
 * Grabador de vídeo MP4 acelerado por hardware para sesiones de juego.
 * Soporta Formato Horizontal (16:9, 1280x720 - por defecto) y Vertical (9:16, 720x1280 - Reels/Stories).
 * Superpone directamente sobre el vídeo los 2 marcadores de arriba (Puntos y Tiempo),
 * los objetivos de reacción (#1, #2, #3), y los símbolos de puntuación (+1, COMBOS, bonus).
 */
class BitmapVideoRecorder(
    private val outputFile: File,
    private val width: Int = 1280,
    private val height: Int = 720,
    private val frameRate: Int = 30
) {
    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private val isRecording = AtomicBoolean(false)
    private var frameCount = 0L
    private var firstPtsUs = -1L
    private val bufferInfo = MediaCodec.BufferInfo()
    private val destRect = Rect(0, 0, width, height)

    // Pre-allocated Paint and Rect objects for live HUD rendering (zero GC allocations per frame)
    private val hudCardRect = RectF()
    private val tempPillRect = RectF()

    // 1. MARCADOR DE PUNTOS
    private val scoreBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 14, 22, 36)
        style = Paint.Style.FILL
    }
    private val scoreBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 47, 178, 201) // Cyan Pro
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(47, 178, 201)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
    }
    private val scoreValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 34f
    }

    // 2. MARCADOR DE TIEMPO
    private val timerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 14, 22, 36)
        style = Paint.Style.FILL
    }
    private val timerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 255, 152, 0) // Sport Orange
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val timerBonusBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118) // Emerald Green highlight
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val timerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 179, 0)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
    }
    private val timerValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 34f
    }
    private val timeBonusTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 0f, Color.argb(200, 0, 230, 118))
    }

    // 3. ESTADO DE BOTE (Centro Superior)
    private val dribbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(225, 6, 78, 59) // Emerald dark
        style = Paint.Style.FILL
    }
    private val dribbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(16, 185, 129)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val dribbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(52, 211, 153)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 13f
        textAlign = Paint.Align.CENTER
    }

    // 4. OBJETIVOS DE REACCIÓN EN PANTALLA (#1, #2, #3)
    private val targetGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 47, 178, 201)
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
    }
    private val targetInnerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(225, 12, 18, 30)
        style = Paint.Style.FILL
    }
    private val targetBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 255, 57) // Neon Volt
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val targetNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }
    private val targetSubtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 255, 57)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    // 5. POPUPS Y COMBOS (+1, COMBO x2!, +5s TIEMPO!)
    private val popupPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 10, 14, 24)
        style = Paint.Style.FILL
    }
    private val popupPillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 226, 255, 57)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val popupRegularTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 255, 57) // Neon Volt para +1
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 34f
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 1f, 2f, Color.argb(220, 0, 0, 0))
    }
    private val popupComboTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 230, 118) // Verde Esmeralda neón para Combos
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 28f
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, Color.argb(240, 0, 230, 118))
    }

    // 6. BRANDING WATERMARK
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(225, 14, 20, 32)
        style = Paint.Style.FILL
    }
    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 152, 0) // SportOrange
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 82, 82) // Red REC dot
        style = Paint.Style.FILL
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f
    }
    private val brandHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 255, 57) // Neon Volt "AI"
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 200, 215, 235)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 11.5f
    }
    private val watermarkRect = RectF()

    @Synchronized
    fun start(): Boolean {
        if (isRecording.get()) return true
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 3_200_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                try {
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                } catch (_: Exception) {}
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()
            codec = encoder

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxerStarted = false
            trackIndex = -1
            frameCount = 0L
            firstPtsUs = -1L
            isRecording.set(true)
            Log.i(TAG, "BitmapVideoRecorder started (${width}x${height}) -> ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BitmapVideoRecorder: ${e.message}", e)
            release()
            return false
        }
    }

    @Synchronized
    fun recordFrame(bitmap: Bitmap, overlayData: VideoGameOverlayData? = null) {
        if (!isRecording.get()) return
        val surface = inputSurface ?: return
        try {
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                surface.lockHardwareCanvas()
            } else {
                surface.lockCanvas(null)
            }

            val targetRatio = width.toFloat() / height.toFloat()
            val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

            if (width > height && srcRatio < 1.0f) {
                // Caso: Cámara en vertical pero grabación en apaisado (16:9):
                // Fondo oscuro con ajuste de altura para que el jugador se vea entero sin recortar
                val scaledWidth = (height * srcRatio).toInt()
                val left = (width - scaledWidth) / 2
                canvas.drawColor(Color.rgb(10, 14, 20))
                canvas.drawBitmap(bitmap, null, Rect(left, 0, left + scaledWidth, height), null)
            } else {
                // Escalado Center-Crop estándar
                val srcRect = if (srcRatio > targetRatio) {
                    val cropWidth = (bitmap.height * targetRatio).toInt().coerceAtMost(bitmap.width)
                    val left = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
                    Rect(left, 0, left + cropWidth, bitmap.height)
                } else {
                    val cropHeight = (bitmap.width / targetRatio).toInt().coerceAtMost(bitmap.height)
                    val top = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
                    Rect(0, top, bitmap.width, top + cropHeight)
                }
                canvas.drawBitmap(bitmap, srcRect, destRect, null)
            }

            // Superponer el HUD del juego si hay datos activos
            if (overlayData != null) {
                drawGameOverlay(canvas, overlayData)
            }

            drawBrandingWatermark(canvas)
            surface.unlockCanvasAndPost(canvas)
            frameCount++
            drainEncoder(endOfStream = false)
        } catch (e: Exception) {
            Log.w(TAG, "Error recording frame to MP4: ${e.message}")
        }
    }

    /**
     * Dibuja los marcadores de arriba (Puntos y Tiempo), los objetivos activos (#1, #2, #3)
     * y los símbolos flotantes (+1, COMBOS, +5s) directamente en el vídeo.
     */
    private fun drawGameOverlay(canvas: android.graphics.Canvas, data: VideoGameOverlayData) {
        val w = width.toFloat()
        val h = height.toFloat()
        val isHorizontal = w > h

        // 1. MARCADOR DE PUNTOS (Arriba a la Izquierda)
        val scoreBadgeW = if (isHorizontal) 160f else 145f
        val scoreBadgeH = if (isHorizontal) 66f else 62f
        val scoreBadgeLeft = if (isHorizontal) 26f else 20f
        val scoreBadgeTop = if (isHorizontal) 22f else 36f
        hudCardRect.set(scoreBadgeLeft, scoreBadgeTop, scoreBadgeLeft + scoreBadgeW, scoreBadgeTop + scoreBadgeH)
        canvas.drawRoundRect(hudCardRect, 14f, 14f, scoreBgPaint)
        canvas.drawRoundRect(hudCardRect, 14f, 14f, scoreBorderPaint)

        canvas.drawText("PUNTOS", scoreBadgeLeft + 16f, scoreBadgeTop + 20f, scoreLabelPaint)
        canvas.drawText("${data.score}", scoreBadgeLeft + 16f, scoreBadgeTop + 54f, scoreValuePaint)

        // 2. MARCADOR DE TIEMPO (Arriba a la Derecha)
        val timerBadgeW = if (isHorizontal) 160f else 145f
        val timerBadgeH = if (isHorizontal) 66f else 62f
        val timerBadgeRight = if (isHorizontal) w - 26f else w - 20f
        val timerBadgeLeft = timerBadgeRight - timerBadgeW
        val timerBadgeTop = if (isHorizontal) 22f else 36f
        hudCardRect.set(timerBadgeLeft, timerBadgeTop, timerBadgeRight, timerBadgeTop + timerBadgeH)
        canvas.drawRoundRect(hudCardRect, 14f, 14f, timerBgPaint)

        val hasRecentBonus = (System.currentTimeMillis() - data.timeBonusTrigger) < 1400L
        if (hasRecentBonus) {
            canvas.drawRoundRect(hudCardRect, 14f, 14f, timerBonusBorderPaint)
            // Tag flotante "+5s ⏱️"
            canvas.drawText("+${data.timeBonusAmount}s", timerBadgeLeft + (timerBadgeW / 2f), timerBadgeTop - 8f, timeBonusTagPaint)
        } else {
            canvas.drawRoundRect(hudCardRect, 14f, 14f, timerBorderPaint)
        }

        val timerStr = String.format("%02d:%02d", data.remainingTimeSec / 60, data.remainingTimeSec % 60)
        canvas.drawText("TIEMPO", timerBadgeLeft + 16f, timerBadgeTop + 20f, timerLabelPaint)
        canvas.drawText(timerStr, timerBadgeLeft + 16f, timerBadgeTop + 54f, timerValuePaint)

        // 3. BADGE DE ESTADO DE BOTE (Centro Superior)
        if (data.isReactionMode && data.isTimerRunning) {
            val dribbleW = 160f
            val dribbleH = 30f
            val dribbleLeft = (w - dribbleW) / 2f
            val dribbleTop = if (isHorizontal) 24f else timerBadgeTop + timerBadgeH + 10f
            tempPillRect.set(dribbleLeft, dribbleTop, dribbleLeft + dribbleW, dribbleTop + dribbleH)
            canvas.drawRoundRect(tempPillRect, 15f, 15f, dribbleBgPaint)
            canvas.drawRoundRect(tempPillRect, 15f, 15f, dribbleBorderPaint)
            val dribbleText = if (data.isDribbleReady) "🏀 BOTE ACTIVO ✓" else "🏀 BOTA EL BALÓN"
            canvas.drawText(dribbleText, dribbleLeft + (dribbleW / 2f), dribbleTop + 20f, dribbleTextPaint)
        }

        // 4. OBJETIVOS DE REACCIÓN EN PANTALLA (#1, #2, #3)
        val unhitPoints = data.activePoints.filter { !it.isHit }
        if (unhitPoints.isNotEmpty() && data.isTimerRunning) {
            val targetRadius = if (isHorizontal) 44f else 40f
            val pulse = ((System.currentTimeMillis() % 1000) / 1000f) * 8f

            for (point in unhitPoints) {
                val cx = point.xNorm * w
                val cy = point.yNorm * h

                // Anillo de pulso exterior
                canvas.drawCircle(cx, cy, targetRadius + pulse, targetGlowPaint)
                // Fondo oscuro del objetivo
                canvas.drawCircle(cx, cy, targetRadius, targetInnerBgPaint)
                // Borde circular vibrante
                canvas.drawCircle(cx, cy, targetRadius, targetBorderPaint)
                // Número central #1, #2, #3
                canvas.drawText("${point.number}", cx, cy + 11f, targetNumberPaint)
                // Etiqueta "TOCA"
                canvas.drawText("TOCA", cx, cy + 26f, targetSubtextPaint)
            }
        }

        // 5. SÍMBOLOS DE PUNTUACIÓN Y COMBOS FLOTANTES (+1, COMBOS, +5s)
        val now = System.currentTimeMillis()
        for (popup in data.popups) {
            val ageMs = now - popup.timestamp
            if (ageMs in 0..1200L) {
                val floatUp = (ageMs / 1200f) * 55f
                val alpha = if (ageMs > 800L) (1f - (ageMs - 800L) / 400f).coerceIn(0f, 1f) else 1f
                val px = popup.xNorm * w
                val py = (popup.yNorm * h) - floatUp

                val isCombo = popup.text.contains("COMBO") || popup.text.contains("+5") || popup.text.contains("🔥")
                val textPaint = if (isCombo) popupComboTextPaint else popupRegularTextPaint
                textPaint.alpha = (alpha * 255).toInt()

                // Fondo para asegurar visibilidad en cualquier pista
                val pillHalfW = if (isCombo) 110f else 60f
                val pillHalfH = if (isCombo) 26f else 22f
                tempPillRect.set(px - pillHalfW, py - pillHalfH - 8f, px + pillHalfW, py + pillHalfH - 8f)
                popupPillBgPaint.alpha = (alpha * 200).toInt()
                popupPillBorderPaint.alpha = (alpha * 180).toInt()
                canvas.drawRoundRect(tempPillRect, 12f, 12f, popupPillBgPaint)
                canvas.drawRoundRect(tempPillRect, 12f, 12f, popupPillBorderPaint)

                canvas.drawText(popup.text, px, py, textPaint)
            }
        }
    }

    private fun drawBrandingWatermark(canvas: android.graphics.Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val isHorizontal = w > h

        // Sello inferior izquierdo 🏀 KANTERA.APP
        val tagW = 140f
        val tagH = 26f
        val tagLeft = if (isHorizontal) 24f else 20f
        val tagBottom = if (isHorizontal) h - 18f else h - 34f
        val tagTop = tagBottom - tagH
        val tagRight = tagLeft + tagW
        watermarkRect.set(tagLeft, tagTop, tagRight, tagBottom)
        canvas.drawRoundRect(watermarkRect, 8f, 8f, badgeBgPaint)
        canvas.drawText("🏀 KANTERA.APP", tagLeft + 10f, tagBottom - 7f, subtitlePaint)

        // Distintivo inferior derecho KANTERA AI
        val badgeW = 210f
        val badgeH = 46f
        val badgeRight = if (isHorizontal) w - 24f else w - 20f
        val badgeLeft = badgeRight - badgeW
        val badgeBottom = if (isHorizontal) h - 18f else h - 34f
        val badgeTop = badgeBottom - badgeH
        watermarkRect.set(badgeLeft, badgeTop, badgeRight, badgeBottom)

        canvas.drawRoundRect(watermarkRect, 12f, 12f, badgeBgPaint)
        canvas.drawRoundRect(watermarkRect, 12f, 12f, badgeBorderPaint)

        val dotX = badgeLeft + 16f
        val dotY = badgeTop + 16f
        canvas.drawCircle(dotX, dotY, 4.5f, dotPaint)

        canvas.drawText("KANTERA", dotX + 10f, badgeTop + 22f, titlePaint)
        canvas.drawText("AI", dotX + 102f, badgeTop + 22f, brandHighlightPaint)
        canvas.drawText("BALL & TOUCH CHALLENGE", dotX + 10f, badgeTop + 37f, subtitlePaint)
    }

    @Synchronized
    fun stop(): File? {
        if (!isRecording.get()) return null
        isRecording.set(false)
        try {
            drainEncoder(endOfStream = true)
        } catch (e: Exception) {
            Log.w(TAG, "Error draining encoder on stop: ${e.message}")
        }
        release()
        return if (outputFile.exists() && outputFile.length() > 0) outputFile else null
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val encoder = codec ?: return
        val mux = muxer ?: return

        if (endOfStream) {
            try {
                encoder.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to signal EOS: ${e.message}")
            }
        }

        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) {
                    Log.w(TAG, "Output format changed twice, ignoring")
                } else {
                    val newFormat = encoder.outputFormat
                    trackIndex = mux.addTrack(newFormat)
                    mux.start()
                    muxerStarted = true
                    Log.i(TAG, "Video muxer started with format: $newFormat")
                }
            } else if (outIndex >= 0) {
                try {
                    val encodedData = encoder.getOutputBuffer(outIndex)
                    if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (muxerStarted && bufferInfo.size > 0) {
                            if (firstPtsUs < 0L) {
                                firstPtsUs = bufferInfo.presentationTimeUs
                            }
                            // Normalizar presentationTimeUs para que empiece exactamente en 0
                            val normalizedPts = (bufferInfo.presentationTimeUs - firstPtsUs).coerceAtLeast(0L)
                            bufferInfo.presentationTimeUs = normalizedPts

                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mux.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing output buffer: ${e.message}")
                    break
                }
            }
        }
    }

    private fun release() {
        try {
            codec?.stop()
        } catch (_: Exception) {}
        try {
            codec?.release()
        } catch (_: Exception) {}
        codec = null

        try {
            inputSurface?.release()
        } catch (_: Exception) {}
        inputSurface = null

        try {
            if (muxerStarted) {
                muxer?.stop()
            }
        } catch (_: Exception) {}
        try {
            muxer?.release()
        } catch (_: Exception) {}
        muxer = null
        muxerStarted = false
        trackIndex = -1
    }
}
