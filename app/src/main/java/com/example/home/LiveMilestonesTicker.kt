package com.example.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.common.UserAvatarImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Modelo para cada Hazaña o Hito de la comunidad en tiempo real.
 */
data class CommunityMilestone(
    val id: String,
    val username: String,
    val actionText: String,
    val badgeTag: String,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val leagueBorderColor: Color,
    val timeAgo: String,
    val avatarUrl: String? = null,
    val avatarRes: Int = R.drawable.avatarchico,
    val initialFistbumps: Int = 0,
    val isFireType: Boolean = false
)

/**
 * TICKER DE HITOS (Live Activity Stream con diseño blanco sutil estilo Apple Fitness).
 *
 * Características de diseño:
 * 1. Fondo blanco puro (#FFFFFF) y filas en blanco/gris sutil (#F8FAFC) sin colores oscuros ni neones.
 * 2. Degradado difuminado blanco suave en bordes superior e inferior.
 * 3. Tipografía nítida en grafito oscuro (#0F172A) y gris neutro (#475569).
 * 4. Insignias pastel discretas sin brillos estridentes.
 * 5. Botones de interacción táctil limpios (👊 / 🔥) con contador de apoyo instantáneo.
 * 6. Desplazamiento automático suave cada 3.5s con control manual por scroll táctil.
 */
@Composable
fun LiveMilestonesTicker(
    modifier: Modifier = Modifier
) {
    // Hitos de la comunidad con tonos suaves y elegantes
    val defaultMilestones = remember {
        listOf(
            CommunityMilestone(
                id = "m1",
                username = "@SwooshKid",
                actionText = "ha subido a LIGA MVP",
                badgeTag = "🏆 MVP",
                badgeBgColor = Color(0xFFFEF3C7),
                badgeTextColor = Color(0xFF92400E),
                leagueBorderColor = Color(0xFFF59E0B),
                timeAgo = "Hace 2 min",
                avatarRes = R.drawable.avatarchico,
                initialFistbumps = 14,
                isFireType = false
            ),
            CommunityMilestone(
                id = "m2",
                username = "@TripleThreat0",
                actionText = "acaba de lograr una racha de 10 días",
                badgeTag = "🔥 10 DÍAS",
                badgeBgColor = Color(0xFFFFEDD5),
                badgeTextColor = Color(0xFFC2410C),
                leagueBorderColor = Color(0xFFF97316),
                timeAgo = "Hace 5 min",
                avatarRes = R.drawable.avatarchica,
                initialFistbumps = 27,
                isFireType = true
            ),
            CommunityMilestone(
                id = "m3",
                username = "@Marc_23",
                actionText = "batió el récord en Dribble King (+160 pts)",
                badgeTag = "⚡ RÉCORD",
                badgeBgColor = Color(0xFFE0F2FE),
                badgeTextColor = Color(0xFF0369A1),
                leagueBorderColor = Color(0xFF38BDF8),
                timeAgo = "Hace 8 min",
                avatarRes = R.drawable.avatarchico,
                initialFistbumps = 19,
                isFireType = false
            ),
            CommunityMilestone(
                id = "m4",
                username = "@Elena_Hoops",
                actionText = "completó Entrenamiento Diario con 94% puntería",
                badgeTag = "🏀 TIRO 94%",
                badgeBgColor = Color(0xFFECFDF5),
                badgeTextColor = Color(0xFF047857),
                leagueBorderColor = Color(0xFF10B981),
                timeAgo = "Hace 12 min",
                avatarRes = R.drawable.avatarchica,
                initialFistbumps = 8,
                isFireType = true
            ),
            CommunityMilestone(
                id = "m5",
                username = "@LukaMagic",
                actionText = "subió a Rango ALL-STAR en el ranking semanal",
                badgeTag = "⭐ ALL-STAR",
                badgeBgColor = Color(0xFFF3E8FF),
                badgeTextColor = Color(0xFF6D28D9),
                leagueBorderColor = Color(0xFF8B5CF6),
                timeAgo = "Hace 16 min",
                avatarRes = R.drawable.avatarchico,
                initialFistbumps = 31,
                isFireType = false
            ),
            CommunityMilestone(
                id = "m6",
                username = "@Pau_9",
                actionText = "encestó 20 triples seguidos en Shot Clock",
                badgeTag = "🎯 20 TRIPLES",
                badgeBgColor = Color(0xFFFEF3C7),
                badgeTextColor = Color(0xFFB45309),
                leagueBorderColor = Color(0xFFF59E0B),
                timeAgo = "Hace 23 min",
                avatarRes = R.drawable.avatarchica,
                initialFistbumps = 45,
                isFireType = true
            ),
            CommunityMilestone(
                id = "m7",
                username = "@Teo_Hoops",
                actionText = "superó los 1,500 pts acumulados de Hype",
                badgeTag = "🚀 1.5K HYPE",
                badgeBgColor = Color(0xFFEFF6FF),
                badgeTextColor = Color(0xFF1D4ED8),
                leagueBorderColor = Color(0xFF3B82F6),
                timeAgo = "Hace 29 min",
                avatarRes = R.drawable.avatarchico,
                initialFistbumps = 12,
                isFireType = false
            ),
            CommunityMilestone(
                id = "m8",
                username = "@Sara_Fast",
                actionText = "completó el Reaction Drill en menos de 180 ms",
                badgeTag = "⚡ 178 MS",
                badgeBgColor = Color(0xFFF0FDFA),
                badgeTextColor = Color(0xFF0F766E),
                leagueBorderColor = Color(0xFF14B8A6),
                timeAgo = "Hace 36 min",
                avatarRes = R.drawable.avatarchica,
                initialFistbumps = 16,
                isFireType = false
            ),
            CommunityMilestone(
                id = "m9",
                username = "@Alex_Crossover",
                actionText = "desbloqueó la insignia especial «Rey del Bote»",
                badgeTag = "👑 MAESTRÍA",
                badgeBgColor = Color(0xFFFDF2F8),
                badgeTextColor = Color(0xFFBE185D),
                leagueBorderColor = Color(0xFFEC4899),
                timeAgo = "Hace 44 min",
                avatarRes = R.drawable.avatarchico,
                initialFistbumps = 23,
                isFireType = true
            ),
            CommunityMilestone(
                id = "m10",
                username = "@Nico_Defense",
                actionText = "consiguió racha de 5 días seguidos entrenando",
                badgeTag = "🔥 5 DÍAS",
                badgeBgColor = Color(0xFFFFEDD5),
                badgeTextColor = Color(0xFFC2410C),
                leagueBorderColor = Color(0xFFFB923C),
                timeAgo = "Hace 52 min",
                avatarRes = R.drawable.avatarchica,
                initialFistbumps = 9,
                isFireType = false
            ),
            CommunityMilestone(
                id = "m11",
                username = "@Clara_Swish",
                actionText = "batió su récord de triples consecutivos (14)",
                badgeTag = "🎯 +14 TRIPLES",
                badgeBgColor = Color(0xFFFEF3C7),
                badgeTextColor = Color(0xFFB45309),
                leagueBorderColor = Color(0xFFF59E0B),
                timeAgo = "Hace 1 h",
                avatarRes = R.drawable.avatarchica,
                initialFistbumps = 18,
                isFireType = true
            ),
            CommunityMilestone(
                id = "m12",
                username = "@Javi_Speed",
                actionText = "alcanzó una velocidad de reacción de 165 ms",
                badgeTag = "⚡ 165 MS",
                badgeBgColor = Color(0xFFE0F2FE),
                badgeTextColor = Color(0xFF0369A1),
                leagueBorderColor = Color(0xFF38BDF8),
                timeAgo = "Hace 1 h",
                avatarRes = R.drawable.avatarchico,
                initialFistbumps = 22,
                isFireType = false
            )
        )
    }

    val userReactions = remember { mutableStateMapOf<String, Boolean>() }
    val reactionCounts = remember {
        mutableStateMapOf<String, Int>().apply {
            defaultMilestones.forEach { put(it.id, it.initialFistbumps) }
        }
    }

    val lazyListState = rememberLazyListState()

    // Desplazamiento automático suave cada 3.5s
    LaunchedEffect(defaultMilestones.size) {
        var currentIndex = 0
        while (isActive) {
            delay(3500)
            if (!lazyListState.isScrollInProgress) {
                currentIndex = (currentIndex + 1) % defaultMilestones.size
                lazyListState.animateScrollToItem(
                    index = currentIndex,
                    scrollOffset = 0
                )
            }
        }
    }

    // Punto pulsante "EN VIVO"
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. CABECERA LIMPIA Y DISCRETA (Sin textos adicionales ni bordes) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ÚLTIMAS HAZAÑAS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Indicador "EN VIVO" solo con punto pulsante y texto (sin contorno ni fondo verde)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "EN VIVO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. CONTENEDOR EN BLANCO SUTIL CON CAPACIDAD PARA 7 FILAS (SIN LÍNEAS DE CONTORNO) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(425.dp) // Ampliado para mostrar 7 filas de golpe
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        ) {
            // LISTA DE HITOS
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = defaultMilestones,
                    key = { _, item -> item.id }
                ) { _, milestone ->
                    val hasReacted = userReactions[milestone.id] == true
                    val currentCount = reactionCounts[milestone.id] ?: milestone.initialFistbumps

                    MilestoneRowItem(
                        milestone = milestone,
                        hasReacted = hasReacted,
                        reactionCount = currentCount,
                        onReactClick = {
                            val nextReacted = !hasReacted
                            userReactions[milestone.id] = nextReacted
                            val delta = if (nextReacted) 1 else -1
                            reactionCounts[milestone.id] = (reactionCounts[milestone.id] ?: milestone.initialFistbumps) + delta
                        }
                    )
                }
            }

            // =========================================================
            // EFECTO DIFUMINADO SUPERIOR (FADE GRADIENT BLANCO)
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color.White.copy(alpha = 0.85f),
                                Color.White.copy(alpha = 0f)
                            )
                        )
                    )
            )

            // =========================================================
            // EFECTO DIFUMINADO INFERIOR (FADE GRADIENT BLANCO)
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0f),
                                Color.White.copy(alpha = 0.85f),
                                Color.White
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Fila compacta con diseño blanco sutil sin líneas de contorno.
 */
@Composable
private fun MilestoneRowItem(
    milestone: CommunityMilestone,
    hasReacted: Boolean,
    reactionCount: Int,
    onReactClick: () -> Unit
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (hasReacted) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "PopAnim"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Avatar con borde suave de liga
            UserAvatarImage(
                avatarUrl = milestone.avatarUrl,
                displayName = milestone.username,
                fallbackDrawable = milestone.avatarRes,
                size = 32.dp,
                borderColor = milestone.leagueBorderColor,
                borderWidth = 1.5.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 2. Texto claro y legible en escala de grises / grafito
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        ) {
                            append(milestone.username)
                        }
                        append(" ")
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.5.sp
                            )
                        ) {
                            append(milestone.actionText)
                        }
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = milestone.timeAgo,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. Insignia sutil pastel (sin brillos ni contorno)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = milestone.badgeBgColor,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = milestone.badgeTag,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = milestone.badgeTextColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                )
            }

            // 4. Botón de choque de puños / fuego limpio (sin contorno)
            val reactionBg = if (hasReacted) Color(0xFFE2E8F0) else Color(0xFFEEF2F6)
            val reactionIcon = if (milestone.isFireType) "🔥" else "👊"

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = reactionBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color(0xFF0F172A))
                    ) { onReactClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = reactionIcon,
                        fontSize = 12.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scaleAnim
                            scaleY = scaleAnim
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$reactionCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasReacted) Color(0xFF0F172A) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
