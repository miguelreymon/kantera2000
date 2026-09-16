package com.example.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.common.UserAvatarImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Sección completa de LEADERBOARD que reproduce fielmente los componentes de las imágenes:
 * 1. Título "LEADERBOARD" con "View More >" y subtítulo
 * 2. Tarjeta del usuario SharpWing3098 en color magenta (#d93b98) con emblema, 👑 #102 y 0XP
 * 3. Contenedor de Clasificación con Podium de los Top 3 (VoltageKid, DeepCross con corona, JaylenFrost)
 * 4. Sección "PLAYERS NEAR YOU" con #101 JustStartedJoe, #102 SharpWing3098 (resaltado) y #103 Emilio
 * 5. Botón de XP BOOST interactivo (modo fake 24 horas con contador regresivo en tiempo real)
 */
@Composable
fun LeaderboardSection(
    userHandle: String = "SharpWing3098",
    userXp: String = "0 Hype",
    userAvatarUrl: String? = null,
    onViewMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Estado del XP Boost (Por defecto desactivado, se activa 24h al pulsar)
    var isBoostActive by remember { mutableStateOf(false) }
    // 24 horas en segundos (24 * 3600 = 86400 segundos)
    var remainingSeconds by remember { mutableLongStateOf(24 * 3600L) }

    // Obtención de datos reales directamente desde el gestor singleton de Supabase
    val realPlayers by com.example.supabase.SupabaseLeaderboardManager.players.collectAsState()
    val hasLoaded by com.example.supabase.SupabaseLeaderboardManager.hasLoaded.collectAsState()

    LaunchedEffect(Unit) {
        // Refresco silencioso en segundo plano sin alterar la memoria existente
        com.example.supabase.SupabaseLeaderboardManager.refreshLeaderboard()
    }

    // Puntuación numérica real del usuario actual
    val parsedUserScore = remember(userXp) {
        userXp.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    // Lista unificada y 100% real con todos los jugadores registrados de Supabase + el usuario actual
    val allRankedPlayers = remember(realPlayers, userHandle, parsedUserScore, userAvatarUrl) {
        val list = realPlayers.toMutableList()
        val existingIdx = list.indexOfFirst {
            it.username.equals(userHandle, ignoreCase = true)
        }

        if (existingIdx != -1) {
            val existing = list[existingIdx]
            val effectiveScore = maxOf(existing.score, parsedUserScore)
            list[existingIdx] = existing.copy(
                score = effectiveScore,
                avatarUrl = userAvatarUrl ?: existing.avatarUrl
            )
        } else {
            list.add(
                com.example.supabase.RealLeaderboardPlayer(
                    userId = "current_user",
                    username = userHandle.ifBlank { "Tú" },
                    avatarUrl = userAvatarUrl,
                    team = "",
                    score = parsedUserScore,
                    rank = 0
                )
            )
        }

        // Ordenar de mayor a menor puntuación real (y alfabético en caso de empate)
        list.sortWith(
            compareByDescending<com.example.supabase.RealLeaderboardPlayer> { it.score }
                .thenBy { it.username.lowercase() }
        )

        // Asignar los puestos oficiales correlativos 1, 2, 3, 4, 5...
        list.mapIndexed { index, player ->
            player.copy(rank = index + 1)
        }
    }

    // Top 3 real del Podium
    val top1 = allRankedPlayers.getOrNull(0)
    val top2 = allRankedPlayers.getOrNull(1)
    val top3 = allRankedPlayers.getOrNull(2)

    val top1Name = top1?.username.orEmpty().ifBlank { if (hasLoaded) "--" else "" }
    val top1Points = top1?.let { "${it.score} pts" } ?: if (hasLoaded) "0 pts" else ""
    val top1AvatarUrl = top1?.avatarUrl

    val top2Name = top2?.username.orEmpty().ifBlank { if (hasLoaded) "--" else "" }
    val top2Points = top2?.let { "${it.score} pts" } ?: if (hasLoaded) "0 pts" else ""
    val top2AvatarUrl = top2?.avatarUrl

    val top3Name = top3?.username.orEmpty().ifBlank { if (hasLoaded) "--" else "" }
    val top3Points = top3?.let { "${it.score} pts" } ?: if (hasLoaded) "0 pts" else ""
    val top3AvatarUrl = top3?.avatarUrl

    // Índice real del usuario actual dentro de la clasificación completa
    val userIndex = remember(allRankedPlayers, userHandle) {
        allRankedPlayers.indexOfFirst {
            it.username.equals(userHandle, ignoreCase = true)
        }
    }

    LaunchedEffect(isBoostActive) {
        while (isBoostActive && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        if (remainingSeconds <= 0) {
            isBoostActive = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. ENCABEZADO: CLASIFICACIÓN + Ver Más > ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CLASIFICACIÓN",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp,
                color = Color(0xFF1E2229)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false)
                    ) { onViewMoreClick() }
                    .padding(4.dp)
            ) {
                Text(
                    text = "Ver Más",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Ver Más",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "¡Acumula puntos, lidera la tabla y llega a lo más alto!",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- CONTENEDOR DE CLASIFICACIÓN (Top 3 Podium + Jugadores Cercanos) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFFF1F5F9))
                .padding(14.dp)
        ) {
            if (!hasLoaded && realPlayers.isEmpty()) {
                // Estado de carga suave en frío: nunca muestra nombres falsos ni saltos
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0284C7),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cargando clasificación de Supabase...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // TOP 3 PODIUM 3D (Solo usuarios registrados de Supabase)
                    ThreeDimensionalPodium(
                        top1Name = top1Name,
                        top1Points = top1Points,
                        top1AvatarUrl = top1AvatarUrl,
                        top2Name = top2Name,
                        top2Points = top2Points,
                        top2AvatarUrl = top2AvatarUrl,
                        top3Name = top3Name,
                        top3Points = top3Points,
                        top3AvatarUrl = top3AvatarUrl,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // --- LISTADO DE 3 FILAS CON EL USUARIO EN EL CENTRO ---
                    // Lógica solicitada:
                    // Si el usuario es el puesto 9, arriba va el 8, en medio el 9 (usuario) y abajo el 10.
                    // Lista 100% real de Supabase, sin el texto de "JUGADORES CERCANOS".
                    val windowStartIndex = remember(allRankedPlayers.size, userIndex) {
                        when {
                            allRankedPlayers.size <= 3 -> 0
                            userIndex <= 0 -> 0
                            userIndex >= allRankedPlayers.size - 1 -> (allRankedPlayers.size - 3).coerceAtLeast(0)
                            else -> userIndex - 1
                        }
                    }

                    val displayPlayers = remember(allRankedPlayers, windowStartIndex) {
                        allRankedPlayers.drop(windowStartIndex).take(3)
                    }

                    if (displayPlayers.isNotEmpty()) {
                        displayPlayers.forEachIndexed { idx, player ->
                            val isMe = player.username.equals(userHandle, ignoreCase = true)
                            val scoreDisplay = if (isMe) userXp else "${player.score} pts"
                            val avatarToUse = if (isMe) userAvatarUrl ?: player.avatarUrl else player.avatarUrl

                            val fallbackRes = if (idx % 2 == 0) R.drawable.avatarchico else R.drawable.avatarchica
                            NearPlayerRowCard(
                                rank = "#${player.rank}",
                                name = player.username,
                                xp = scoreDisplay,
                                isCurrentPlayer = isMe,
                                avatar = {
                                    UserAvatarImage(
                                        avatarUrl = avatarToUse,
                                        displayName = player.username,
                                        fallbackDrawable = fallbackRes,
                                        size = 34.dp,
                                        borderColor = if (isMe) Color(0xFFE15F25) else null,
                                        borderWidth = if (isMe) 1.5.dp else 0.dp
                                    )
                                }
                            )

                            if (idx < displayPlayers.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. BOTÓN DE XP BOOST (Activable 24 horas modo fake con timer) ---
        XpBoostButton(
            isActive = isBoostActive,
            remainingSeconds = remainingSeconds,
            onToggle = {
                if (!isBoostActive) {
                    isBoostActive = true
                    remainingSeconds = 24 * 3600L
                } else {
                    // Si ya estaba activo, reiniciar a 24 horas completas
                    remainingSeconds = 24 * 3600L
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Activate once every 24 hours — boosts Hype for 1 hour.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Tarjeta vibrante magenta del usuario en la parte superior del Leaderboard.
 */
@Composable
private fun UserLeaderboardHighlightCard(
    userHandle: String,
    rank: String,
    xp: String,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("user_leaderboard_highlight_card")
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFD93B98))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emblema circular multicolor Hoopstars o foto real del usuario
            UserAvatarImage(
                avatarUrl = avatarUrl,
                displayName = userHandle,
                size = 44.dp,
                borderColor = Color.White,
                borderWidth = 2.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = userHandle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Pill amarilla con corona y posición (#102)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFACC15))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "👑",
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = rank,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Pill blanca con Hype / XP ("0 Hype") con icono cohete
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.cohete),
                        contentDescription = "Hype",
                        modifier = Modifier.size(13.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = xp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual de los 3 jugadores del podio (#1, #2, #3).
 */
/**
 * Formatea los puntos para mostrar sólo los dígitos limpios en el podio (ej. 5,782).
 */
private fun formatPodiumPoints(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.isNotEmpty()) {
        try {
            val num = digits.toLong()
            String.format(Locale.getDefault(), "%,d", num)
        } catch (_: Exception) {
            digits
        }
    } else {
        raw
    }
}

/**
 * Icono vectorial de baloncesto sobre el podio 3D con costuras y borde blanco.
 */
@Composable
fun PodiumBasketballIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Círculo relleno de color naranja baloncesto brillante
        drawCircle(
            color = Color(0xFFFF6D00),
            radius = radius,
            center = center
        )
        // Borde exterior blanco
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 1.6.dp.toPx())
        )
        val seamStroke = Stroke(width = 1.3.dp.toPx())
        // Línea horizontal
        drawLine(
            color = Color.White,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.3.dp.toPx()
        )
        // Línea vertical
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.3.dp.toPx()
        )
        // Costura curva izquierda
        val leftArcPath = Path().apply {
            moveTo(center.x - radius * 0.72f, center.y - radius * 0.72f)
            quadraticTo(center.x - radius * 0.12f, center.y, center.x - radius * 0.72f, center.y + radius * 0.72f)
        }
        drawPath(leftArcPath, color = Color.White, style = seamStroke)
        // Costura curva derecha
        val rightArcPath = Path().apply {
            moveTo(center.x + radius * 0.72f, center.y - radius * 0.72f)
            quadraticTo(center.x + radius * 0.12f, center.y, center.x + radius * 0.72f, center.y + radius * 0.72f)
        }
        drawPath(rightArcPath, color = Color.White, style = seamStroke)
    }
}

/**
 * Pilar volumétrico 3D en perspectiva isométrica dorada.
 * Incluye faceta superior reflectante, cara frontal con número gigante estilizado en blanco
 * y sombra lateral de profundidad según la perspectiva.
 */
@Composable
private fun Podium3DPillar(
    rank: Int,
    height: Dp,
    points: String,
    rankFontSize: TextUnit,
    pointsFontSize: TextUnit,
    basketballSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(height)
    ) {
        // Renderizado del volumen 3D con Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val topH = 34.dp.toPx()

            when (rank) {
                1 -> {
                    // PUESTO 1 (CENTRO): Adelantado respecto a los lados
                    // Faceta superior (plataforma dorada brillante)
                    val topFace = Path().apply {
                        moveTo(w * 0.08f, 0f)
                        lineTo(w * 0.92f, 0f)
                        lineTo(w * 0.98f, topH)
                        lineTo(w * 0.02f, topH)
                        close()
                    }
                    drawPath(
                        topFace,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFEE58), Color(0xFFFFCA28))
                        )
                    )

                    // Cara frontal (prisma dorado cálido con suave degradado hacia abajo)
                    val frontFace = Path().apply {
                        moveTo(w * 0.02f, topH)
                        lineTo(w * 0.98f, topH)
                        lineTo(w * 0.94f, h)
                        lineTo(w * 0.06f, h)
                        close()
                    }
                    drawPath(
                        frontFace,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFB300), Color(0xFFFFA000), Color(0xFFFF8F00))
                        )
                    )

                    // Resalte luminoso en la arista superior
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(w * 0.08f, 0f),
                        end = Offset(w * 0.92f, 0f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f),
                        start = Offset(w * 0.02f, topH),
                        end = Offset(w * 0.98f, topH),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
                2 -> {
                    // PUESTO 2 (IZQUIERDA): Inclinado hacia el centro
                    // Faceta superior
                    val topFace = Path().apply {
                        moveTo(w * 0.04f, topH * 0.15f)
                        lineTo(w * 0.96f, 0f)
                        lineTo(w * 1.0f, topH * 0.9f)
                        lineTo(0f, topH)
                        close()
                    }
                    drawPath(
                        topFace,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFEE58), Color(0xFFFFCA28))
                        )
                    )

                    // Cara frontal
                    val frontFace = Path().apply {
                        moveTo(0f, topH)
                        lineTo(w * 1.0f, topH * 0.9f)
                        lineTo(w * 0.96f, h)
                        lineTo(w * 0.04f, h)
                        close()
                    }
                    drawPath(
                        frontFace,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFB300), Color(0xFFFFA000), Color(0xFFFF8F00))
                        )
                    )

                    // Faceta lateral derecha sombreada (bajo el puesto #1)
                    val rightShadow = Path().apply {
                        moveTo(w * 0.88f, topH * 0.9f)
                        lineTo(w * 1.0f, topH * 0.9f)
                        lineTo(w * 0.96f, h)
                        lineTo(w * 0.84f, h)
                        close()
                    }
                    drawPath(rightShadow, color = Color(0xFFD97706).copy(alpha = 0.5f))

                    // Línea de arista
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(0f, topH),
                        end = Offset(w * 0.88f, topH * 0.9f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                else -> {
                    // PUESTO 3 (DERECHA): Inclinado hacia el centro
                    // Faceta superior
                    val topFace = Path().apply {
                        moveTo(w * 0.04f, 0f)
                        lineTo(w * 0.96f, topH * 0.15f)
                        lineTo(w * 1.0f, topH)
                        lineTo(0f, topH * 0.9f)
                        close()
                    }
                    drawPath(
                        topFace,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFEE58), Color(0xFFFFCA28))
                        )
                    )

                    // Cara frontal
                    val frontFace = Path().apply {
                        moveTo(0f, topH * 0.9f)
                        lineTo(w * 1.0f, topH)
                        lineTo(w * 0.96f, h)
                        lineTo(w * 0.04f, h)
                        close()
                    }
                    drawPath(
                        frontFace,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFB300), Color(0xFFFFA000), Color(0xFFFF8F00))
                        )
                    )

                    // Faceta lateral izquierda sombreada (bajo el puesto #1)
                    val leftShadow = Path().apply {
                        moveTo(0f, topH * 0.9f)
                        lineTo(w * 0.12f, topH * 0.9f)
                        lineTo(w * 0.16f, h)
                        lineTo(w * 0.04f, h)
                        close()
                    }
                    drawPath(leftShadow, color = Color(0xFFD97706).copy(alpha = 0.5f))

                    // Línea de arista
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(w * 0.12f, topH * 0.9f),
                        end = Offset(w * 1.0f, topH),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // Elementos sobre el pilar:
        // A) En la plataforma superior: Balón de baloncesto + Puntos en la misma línea
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PodiumBasketballIcon(modifier = Modifier.size(basketballSize))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = points,
                fontSize = pointsFontSize,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1E3A8A), // Azul marino deportivo nítido sobre dorado
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // B) En la cara frontal: Gran número 1, 2 o 3 estilizado en blanco con sombra
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 34.dp, bottom = 4.dp)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                fontSize = rankFontSize,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x38000000),
                        offset = Offset(2f, 3f),
                        blurRadius = 4f
                    )
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Columna individual de jugador en el podio (Corona/Espacio, Avatar circular, Nombre, Pilar 3D).
 * El avatar y nombre se posicionan muy cerca de la plataforma superior y del balón con puntos.
 */
@Composable
private fun PodiumColumn(
    rank: Int,
    name: String,
    points: String,
    avatarUrl: String?,
    pillarHeight: Dp,
    avatarSize: Dp,
    hasCrown: Boolean = false,
    rankFontSize: TextUnit,
    pointsFontSize: TextUnit,
    basketballSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // 1. Corona para el puesto #1
        if (hasCrown) {
            Text(
                text = "👑",
                fontSize = 20.sp,
                modifier = Modifier.offset(y = 3.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Avatar circular flotante asentado cerca de la plataforma
        Box(
            modifier = Modifier
                .size(avatarSize)
                .shadow(4.dp, CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val podiumFallback = if (rank % 2 == 0) R.drawable.avatarchica else R.drawable.avatarchico
            UserAvatarImage(
                avatarUrl = avatarUrl,
                displayName = name,
                fallbackDrawable = podiumFallback,
                size = avatarSize
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 3. Nombre del jugador (en cursiva y negrita, bajado pegado a la plataforma)
        Text(
            text = name,
            fontSize = if (rank == 1) 11.5.sp else 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )

        Spacer(modifier = Modifier.height(1.dp))

        // 4. Pilar 3D con balón, puntos y gran número frontal
        Podium3DPillar(
            rank = rank,
            height = pillarHeight,
            points = points,
            rankFontSize = rankFontSize,
            pointsFontSize = pointsFontSize,
            basketballSize = basketballSize,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Podio 3D completo que reproduce la composición de la imagen de referencia:
 * - Puesto #2 a la izquierda (altura media)
 * - Puesto #1 en el centro (más alto, con corona, pilar en primer plano)
 * - Puesto #3 a la derecha (más bajo)
 * - Animación de entrada coordinada y suave la primera vez que entra en pantalla al deslizarse hacia abajo.
 * - Una vez ejecutada, queda fija para siempre para ese usuario.
 */
@Composable
fun ThreeDimensionalPodium(
    top1Name: String,
    top1Points: String,
    top1AvatarUrl: String?,
    top2Name: String,
    top2Points: String,
    top2AvatarUrl: String?,
    top3Name: String,
    top3Points: String,
    top3AvatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialOffset23 = 120f
    val initialOffset1 = 150f

    val offsetY2 = remember { Animatable(initialOffset23) }
    val alpha2 = remember { Animatable(0f) }

    val offsetY3 = remember { Animatable(initialOffset23) }
    val alpha3 = remember { Animatable(0f) }

    val offsetY1 = remember { Animatable(initialOffset1) }
    val alpha1 = remember { Animatable(0f) }

    var animationTriggered by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val screenHeight = context.resources.displayMetrics.heightPixels

                // Si el usuario regresa arriba del todo en la página y el podio queda fuera de pantalla por abajo,
                // rearmamos la animación para que cuando vuelva a bajar hacia él vuelva a animar
                if (bounds.top > screenHeight * 1.05f && animationTriggered) {
                    animationTriggered = false
                    coroutineScope.launch {
                        offsetY2.snapTo(initialOffset23)
                        alpha2.snapTo(0f)
                        offsetY3.snapTo(initialOffset23)
                        alpha3.snapTo(0f)
                        offsetY1.snapTo(initialOffset1)
                        alpha1.snapTo(0f)
                    }
                } else if (bounds.top < screenHeight * 0.90f && bounds.bottom > 0 && !animationTriggered) {
                    animationTriggered = true

                    coroutineScope.launch {
                        val riseSpec23 = tween<Float>(durationMillis = 650, easing = FastOutSlowInEasing)
                        val alphaSpec23 = tween<Float>(durationMillis = 400, easing = LinearEasing)

                        // 1. Suben primero el número 3 y el número 2 al mismo tiempo hasta su posición
                        launch { offsetY2.animateTo(0f, riseSpec23) }
                        launch { alpha2.animateTo(1f, alphaSpec23) }
                        launch { offsetY3.animateTo(0f, riseSpec23) }
                        launch { alpha3.animateTo(1f, alphaSpec23) }

                        // 2. Y después, súper coordinado y suave sin sustos, sube el número 1 a su posición más alta
                        delay(260)
                        launch {
                            offsetY1.animateTo(0f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
                        }
                        launch {
                            alpha1.animateTo(1f, tween(durationMillis = 450, easing = LinearEasing))
                        }
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            // #2 PUESTO (IZQUIERDA)
            PodiumColumn(
                rank = 2,
                name = top2Name,
                points = formatPodiumPoints(top2Points),
                avatarUrl = top2AvatarUrl,
                pillarHeight = 120.dp,
                avatarSize = 58.dp,
                hasCrown = false,
                rankFontSize = 50.sp,
                pointsFontSize = 10.5.sp,
                basketballSize = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationY = offsetY2.value.dp.toPx()
                        alpha = alpha2.value
                    }
            )

            // #1 PUESTO (CENTRO, MÁS ALTO Y EN PRIMER PLANO)
            PodiumColumn(
                rank = 1,
                name = top1Name,
                points = formatPodiumPoints(top1Points),
                avatarUrl = top1AvatarUrl,
                pillarHeight = 160.dp,
                avatarSize = 66.dp,
                hasCrown = true,
                rankFontSize = 60.sp,
                pointsFontSize = 11.5.sp,
                basketballSize = 16.dp,
                modifier = Modifier
                    .weight(1.15f)
                    .zIndex(2f)
                    .graphicsLayer {
                        translationY = offsetY1.value.dp.toPx()
                        alpha = alpha1.value
                    }
            )

            // #3 PUESTO (DERECHA)
            PodiumColumn(
                rank = 3,
                name = top3Name,
                points = formatPodiumPoints(top3Points),
                avatarUrl = top3AvatarUrl,
                pillarHeight = 98.dp,
                avatarSize = 56.dp,
                hasCrown = false,
                rankFontSize = 42.sp,
                pointsFontSize = 10.5.sp,
                basketballSize = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationY = offsetY3.value.dp.toPx()
                        alpha = alpha3.value
                    }
            )
        }

        // Base sutil del podio para asentar las 3 columnas en el suelo
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .alpha(alpha2.value)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x30E65100),
                            Color(0x50FFA000),
                            Color(0x30E65100),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Fila para cada jugador en la sección "PLAYERS NEAR YOU".
 * Si es el jugador actual, incluye el borde magenta distintivo.
 */
@Composable
private fun NearPlayerRowCard(
    rank: String,
    name: String,
    xp: String,
    isCurrentPlayer: Boolean,
    avatar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBorder = if (isCurrentPlayer) {
        BorderStroke(2.dp, Color(0xFFE15F25))
    } else {
        BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    val textColor = if (isCurrentPlayer) Color(0xFFE15F25) else Color(0xFF1E293B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                modifier = Modifier.width(42.dp)
            )

            avatar()

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = xp,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

/**
 * Botón para activar o ver el estado del XP Boost (con temporizador regresivo de 24h).
 */
@Composable
private fun XpBoostButton(
    isActive: Boolean,
    remainingSeconds: Long,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    val timerText = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)

    Box(
        modifier = modifier
            .testTag("xp_boost_action_button")
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) Color(0xFFE2E8F0) else Color(0xFF1E293B))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White)
            ) { onToggle() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚡",
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) {
                    "HYPE BOOST ACTIVE — $timerText"
                } else {
                    "BOOST HYPE"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = if (isActive) Color(0xFF64748B) else Color.White
            )
        }
    }
}

// ==========================================
// AVATARES PERSONALIZADOS PARA EL LEADERBOARD
// ==========================================

/**
 * Avatar para VoltageKid (Niño con pelo naranja llamativo y expresión entusiasta).
 */
@Composable
fun VoltageKidAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFFEF08A))
            .border(2.dp, Color(0xFFF59E0B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Pelo naranja puntiagudo superior
            val hairPath = Path().apply {
                moveTo(w * 0.2f, h * 0.45f)
                lineTo(w * 0.15f, h * 0.25f)
                lineTo(w * 0.35f, h * 0.15f)
                lineTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.65f, h * 0.15f)
                lineTo(w * 0.85f, h * 0.25f)
                lineTo(w * 0.8f, h * 0.45f)
                close()
            }
            drawPath(hairPath, color = Color(0xFFEA580C))

            // Cara color piel
            drawCircle(
                color = Color(0xFFFFD1B3),
                radius = w * 0.34f,
                center = Offset(w * 0.5f, h * 0.55f)
            )

            // Flequillo
            drawCircle(
                color = Color(0xFFEA580C),
                radius = w * 0.18f,
                center = Offset(w * 0.4f, h * 0.38f)
            )
            drawCircle(
                color = Color(0xFFEA580C),
                radius = w * 0.15f,
                center = Offset(w * 0.6f, h * 0.38f)
            )

            // Ojos grandes animados
            drawCircle(
                color = Color.White,
                radius = w * 0.09f,
                center = Offset(w * 0.4f, h * 0.54f)
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.05f,
                center = Offset(w * 0.4f, h * 0.54f)
            )

            drawCircle(
                color = Color.White,
                radius = w * 0.09f,
                center = Offset(w * 0.6f, h * 0.54f)
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.05f,
                center = Offset(w * 0.6f, h * 0.54f)
            )

            // Sonrisa
            drawArc(
                color = Color(0xFF9A3412),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.66f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.10f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Avatar para DeepCross (Fondo rosa, gafas elegantes, barba estilizada y sonrisa).
 */
@Composable
fun DeepCrossAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFFCE7F3))
            .border(2.dp, Color(0xFFF43F5E), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Pelo / turbante superior suave
            drawCircle(
                color = Color(0xFFF472B6),
                radius = w * 0.36f,
                center = Offset(w * 0.5f, h * 0.32f)
            )

            // Cara
            drawCircle(
                color = Color(0xFFD4A373),
                radius = w * 0.32f,
                center = Offset(w * 0.5f, h * 0.55f)
            )

            // Barba negra elegante
            val beardPath = Path().apply {
                moveTo(w * 0.26f, h * 0.56f)
                lineTo(w * 0.28f, h * 0.76f)
                lineTo(w * 0.5f, h * 0.88f)
                lineTo(w * 0.72f, h * 0.76f)
                lineTo(w * 0.74f, h * 0.56f)
                close()
            }
            drawPath(beardPath, color = Color(0xFF1E1E24))

            // Gafas circulares rojas
            drawCircle(
                color = Color(0xFFE11D48),
                radius = w * 0.12f,
                center = Offset(w * 0.38f, h * 0.52f),
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFE11D48),
                radius = w * 0.12f,
                center = Offset(w * 0.62f, h * 0.52f),
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawLine(
                color = Color(0xFFE11D48),
                start = Offset(w * 0.48f, h * 0.52f),
                end = Offset(w * 0.52f, h * 0.52f),
                strokeWidth = 2.5.dp.toPx()
            )

            // Ojos sonrientes detrás de gafas
            drawCircle(
                color = Color(0xFF1E1E24),
                radius = w * 0.04f,
                center = Offset(w * 0.38f, h * 0.52f)
            )
            drawCircle(
                color = Color(0xFF1E1E24),
                radius = w * 0.04f,
                center = Offset(w * 0.62f, h * 0.52f)
            )

            // Sonrisa blanca en la barba
            drawArc(
                color = Color.White,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.42f, h * 0.68f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.08f)
            )
        }
    }
}

/**
 * Avatar para JaylenFrost (Estilo pixel-art / rubio gamer).
 */
@Composable
fun JaylenFrostAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFE0F2FE))
            .border(2.dp, Color(0xFF38BDF8), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cabeza base tipo pixel / bloque
            drawRect(
                color = Color(0xFFFFDBB5),
                topLeft = Offset(w * 0.25f, h * 0.28f),
                size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.50f)
            )

            // Pelo pixelado rubio
            drawRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(w * 0.22f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.22f)
            )
            drawRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(w * 0.22f, h * 0.40f),
                size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.20f)
            )
            drawRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(w * 0.66f, h * 0.40f),
                size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.20f)
            )

            // Ojos cuadrados estilo pixel
            drawRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(w * 0.35f, h * 0.46f),
                size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.08f)
            )
            drawRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(w * 0.57f, h * 0.46f),
                size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.08f)
            )

            // Boca pixel
            drawRect(
                color = Color(0xFFB91C1C),
                topLeft = Offset(w * 0.45f, h * 0.64f),
                size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.05f)
            )
        }
    }
}

/**
 * Avatar para JustStartedJoe (Niño preocupado/nervioso).
 */
@Composable
fun JustStartedJoeAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFE0F2FE))
            .border(1.5.dp, Color(0xFF93C5FD), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cara piel clara
            drawCircle(
                color = Color(0xFFFFDFC4),
                radius = w * 0.38f,
                center = Offset(w * 0.5f, h * 0.52f)
            )

            // Pelo castaño claro fino
            drawArc(
                color = Color(0xFF92400E),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.25f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.32f)
            )

            // Cejas tristes/preocupadas
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(w * 0.32f, h * 0.44f),
                end = Offset(w * 0.44f, h * 0.40f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(w * 0.56f, h * 0.40f),
                end = Offset(w * 0.68f, h * 0.44f),
                strokeWidth = 2.dp.toPx()
            )

            // Ojitos caídos
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.04f,
                center = Offset(w * 0.38f, h * 0.50f)
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.04f,
                center = Offset(w * 0.62f, h * 0.50f)
            )

            // Gota de sudor / nervio
            drawCircle(
                color = Color(0xFF60A5FA),
                radius = w * 0.06f,
                center = Offset(w * 0.74f, h * 0.42f)
            )

            // Boquita ondulada triste
            drawArc(
                color = Color(0xFF78350F),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.66f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.08f),
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}

/**
 * Avatar pequeño Hoopstars con el degradado y el balón con "H★".
 */
@Composable
fun HoopstarsAvatarSmall(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFFF2A85),
                        Color(0xFFFFD600),
                        Color(0xFF00E5FF)
                    )
                )
            )
            .padding(2.dp)
            .clip(CircleShape)
            .background(Color(0xFF00BCD4)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                color = Color(0xFFFF4081),
                radius = w * 0.45f,
                style = Stroke(width = 1.8f)
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1.8f
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h),
                strokeWidth = 1.8f
            )
        }

        Text(
            text = "H★",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF111827)
        )
    }
}
