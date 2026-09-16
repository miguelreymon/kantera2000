package com.example.profile

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.R
import com.example.home.GameTrainToggle
import com.example.stats.PlayerStats
import com.example.stats.PlayerStatsManager
import com.example.ui.common.UserAvatarImage
import com.example.vision.SavedVideoAnalysis
import com.example.vision.SavedVideoManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shape geométrico vectorial para los escudos de liga y logros.
 */
val ShieldShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.12f, 0f)
    lineTo(w * 0.88f, 0f)
    cubicTo(w * 0.98f, 0f, w, 0.06f * h, w, 0.14f * h)
    lineTo(w, h * 0.58f)
    cubicTo(w, h * 0.80f, w * 0.65f, h * 0.93f, w * 0.5f, h)
    cubicTo(w * 0.35f, h * 0.93f, 0f, h * 0.80f, 0f, h * 0.58f)
    lineTo(0f, 0.14f * h)
    cubicTo(0f, 0.06f * h, w * 0.02f, 0f, w * 0.12f, 0f)
    close()
}

/**
 * Modelo de datos de Logro (Badge).
 */
data class ProfileAchievement(
    val id: String,
    val titleEs: String,
    val descriptionEs: String,
    val xpReward: Int,
    val isUnlocked: (PlayerStats, List<SavedVideoAnalysis>) -> Boolean,
    val progressText: (PlayerStats, List<SavedVideoAnalysis>) -> String
)

/**
 * Pantalla completa de Perfil del Usuario.
 * Replica de manera fidedigna el diseño de la captura de pantalla:
 * - Header con badge de rango, píldora de XP y botón de configuración
 * - Fila de información de usuario con avatar artístico, nombre con botón de edición y estadísticas (Siguiendo, Seguidores, Vídeos)
 * - Barra de progreso de liga ("LIGA ROOKIE 0/499")
 * - 4 Escudos de liga (ROOKIE, ALL-STAR, MVP, GOAT)
 * - Pestañas "Vídeos" y "Logros"
 * - Sección de vídeos guardados localmente
 * - Sección de escudos/logros interactivos con animación 3D al girar y cambio de color al conseguirlos.
 */
@Composable
fun FullProfileScreen(
    onOpenSettings: () -> Unit,
    isGameMode: Boolean = true,
    onToggleMode: (Boolean) -> Unit = {},
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerStats by PlayerStatsManager.stats.collectAsState()
    val savedVideoManager = remember { SavedVideoManager(context) }
    var savedVideos by remember { mutableStateOf<List<SavedVideoAnalysis>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Player Card (NBA 2K / FUT), 1: Logros, 2: Vídeos
    var showEditNameDialog by remember { mutableStateOf(false) }

    // Cargar vídeos locales guardados
    LaunchedEffect(Unit) {
        savedVideos = savedVideoManager.getAllSaved()
    }

    // Lista de logros disponibles en español
    val achievements = remember {
        listOf(
            ProfileAchievement(
                id = "first_100_xp",
                titleEs = "PRIMEROS PASOS",
                descriptionEs = "CONSIGUE TUS PRIMEROS 100 XP EN CUALQUIER MODO DE JUEGO.",
                xpReward = 100,
                isUnlocked = { stats, _ -> stats.totalXp >= 100 },
                progressText = { stats, _ -> "${stats.totalXp.coerceAtMost(100)} / 100 XP" }
            ),
            ProfileAchievement(
                id = "1st_place",
                titleEs = "1ER LUGAR",
                descriptionEs = "CONSIGUE TU PRIMER RÉCORD O PUNTUACIÓN DESTACADA EN UN MINIJUEGO.",
                xpReward = 175,
                isUnlocked = { stats, _ ->
                    stats.reactionPointsBest > 0 || stats.dribbleComboBest > 0 || stats.defendZoneBest > 0
                },
                progressText = { stats, _ ->
                    val best = maxOf(stats.reactionPointsBest, stats.dribbleComboBest, stats.defendZoneBest)
                    if (best > 0) "Récord: $best pts" else "Sin récord aún"
                }
            ),
            ProfileAchievement(
                id = "talk_of_court",
                titleEs = "REY DE LA PISTA",
                descriptionEs = "COMPLETA AL MENOS 3 SESIONES COMPLETAS DE JUEGO O ENTRENAMIENTO.",
                xpReward = 100,
                isUnlocked = { stats, _ ->
                    (stats.reactionPointsGames + stats.dribbleComboGames + stats.defendZoneGames + stats.shootingSessions) >= 3
                },
                progressText = { stats, _ ->
                    val totalGames = stats.reactionPointsGames + stats.dribbleComboGames + stats.defendZoneGames + stats.shootingSessions
                    "${totalGames.coerceAtMost(3)} / 3 partidas"
                }
            ),
            ProfileAchievement(
                id = "publish_5_videos",
                titleEs = "CINCO VÍDEOS",
                descriptionEs = "GUARDA O ANALIZA 5 VÍDEOS DE JUGADAS EN LA CANCHA.",
                xpReward = 75,
                isUnlocked = { stats, videos ->
                    videos.size >= 5 || stats.shootingSessions >= 5
                },
                progressText = { stats, videos ->
                    val count = maxOf(videos.size, stats.shootingSessions)
                    "${count.coerceAtMost(5)} / 5 vídeos"
                }
            ),
            ProfileAchievement(
                id = "court_clout",
                titleEs = "FAMA EN LA CANCHA",
                descriptionEs = "ALCANZA EL RANGO PRO SUPERANDO LOS 250 XP TOTALES.",
                xpReward = 150,
                isUnlocked = { stats, _ -> stats.totalXp >= 250 },
                progressText = { stats, _ -> "${stats.totalXp.coerceAtMost(250)} / 250 XP" }
            ),
            ProfileAchievement(
                id = "defend_iron",
                titleEs = "DEFENSA DE HIERRO",
                descriptionEs = "SOBREVIVE Y PROTEGE LA ZONA EN DEFEND THE ZONE.",
                xpReward = 125,
                isUnlocked = { stats, _ -> stats.defendZoneBest >= 15 },
                progressText = { stats, _ -> "Récord: ${stats.defendZoneBest} pts" }
            ),
            ProfileAchievement(
                id = "dribble_master",
                titleEs = "MAESTRO DEL DRIBLE",
                descriptionEs = "ENCADENA COMBOS DE CROSSOVER EN DRIBBLE COMBO.",
                xpReward = 120,
                isUnlocked = { stats, _ -> stats.dribbleComboBest >= 20 || stats.dribbleCrossovers >= 5 },
                progressText = { stats, _ -> "Récord: ${stats.dribbleComboBest} pts" }
            ),
            ProfileAchievement(
                id = "sniper_shot",
                titleEs = "FRANCOTIRADOR",
                descriptionEs = "ANOTA 5 CANASTAS EN LA SESIÓN DE TIRO CON ANÁLISIS.",
                xpReward = 200,
                isUnlocked = { stats, _ -> stats.shootingMakes >= 5 || stats.shootingXp >= 80 },
                progressText = { stats, _ -> "${stats.shootingMakes.coerceAtMost(5)} / 5 canastas" }
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .testTag("full_profile_screen")
    ) {
        // ==========================================
        // 1. HEADER SUPERIOR FIJO: Exactamente la misma posición, padding y altura que en Inicio y Workout
        // ==========================================
        ProfileTopHeader(
            rankTitle = playerStats.rankTitle,
            totalXp = playerStats.totalXp,
            isGameMode = isGameMode,
            onToggleMode = onToggleMode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(44.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // ==========================================
            // 2. FILA DE PERFIL (Avatar unificado, Nombre, Ajustes a la derecha, Stats)
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileUserCard(
                    playerName = playerStats.playerName,
                    avatarUrl = avatarUrl,
                    isGameMode = isGameMode,
                    totalXp = playerStats.totalXp,
                    videoCount = savedVideos.size,
                    onEditNameClick = { showEditNameDialog = true },
                    onOpenSettings = onOpenSettings,
                    onPlayerCardClick = { selectedTab = 0 }
                )
            }

            // ==========================================
            // 3. BARRA DE PROGRESO DE LIGA
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileLeagueProgress(
                    rankTitle = playerStats.rankTitle,
                    totalXp = playerStats.totalXp
                )
            }

            // ==========================================
            // 4. FILA DE 4 ESCUDOS DE LIGA
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileLeagueBadgesRow(totalXp = playerStats.totalXp)
            }

            // ==========================================
            // 5. PESTAÑAS (3 SECCIONES): PLAYER CARD | LOGROS | VÍDEOS
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileTabsRow(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it }
                )
            }

            // ==========================================
            // 6. CONTENIDO SEGÚN LA PESTAÑA SELECCIONADA
            // ==========================================
            when (selectedTab) {
                0 -> {
                    // Pestaña 1: LA "PLAYER CARD" (Carta coleccionable interactiva estilo NBA 2K / FIFA FUT)
                    item(span = { GridItemSpan(2) }) {
                        PlayerCardSection(
                            playerStats = playerStats,
                            avatarUrl = avatarUrl
                        )
                    }
                }
                1 -> {
                    // Pestaña 2: LOGROS (Escudos interactivos con giro 3D)
                    items(achievements, key = { it.id }, span = { GridItemSpan(1) }) { achievement ->
                        val unlocked = achievement.isUnlocked(playerStats, savedVideos)
                        ProfileAchievementShieldCard(
                            achievement = achievement,
                            isUnlocked = unlocked,
                            progressText = achievement.progressText(playerStats, savedVideos)
                        )
                    }
                }
                2 -> {
                    // Pestaña 3: VÍDEOS (Vídeos locales guardados)
                    if (savedVideos.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            ProfileVideosEmptyState()
                        }
                    } else {
                        items(savedVideos, key = { it.id }, span = { GridItemSpan(1) }) { video ->
                            ProfileVideoItemCard(video = video)
                        }
                    }
                }
            }
        }

        // Diálogo para editar el nombre de usuario
        if (showEditNameDialog) {
            EditPlayerNameDialog(
                currentName = playerStats.playerName,
                onDismiss = { showEditNameDialog = false },
                onSave = { newName ->
                    PlayerStatsManager.updatePlayerName(newName)
                    showEditNameDialog = false
                }
            )
        }
    }
}

/**
 * Encabezado superior con la píldora de Rango, píldora de XP a la izquierda y selector GAME/PRO a la derecha.
 */
@Composable
private fun ProfileTopHeader(
    rankTitle: String,
    totalXp: Int,
    isGameMode: Boolean,
    onToggleMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LADO IZQUIERDO: Píldora de Rango (ej. 🏆 ROOKIE) + Píldora de XP
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Píldora de Rango (ej. 🏆 ROOKIE)
            Surface(
                shape = CircleShape,
                color = if (isGameMode) Color(0xFFF1F5F9) else Color(0xFF1E2433),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isGameMode) Color(0xFFE2E8F0) else Color(0xFF334155)),
                modifier = Modifier.height(30.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rankTitle.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGameMode) Color(0xFF1E293B) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Píldora de Hype (en Modo GAME con icono cohete) o XP (en Modo TRAIN/PRO)
            Surface(
                shape = CircleShape,
                color = if (isGameMode) Color(0xFFFDF2F8) else Color(0xFF28182A),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD93B98)),
                modifier = Modifier.height(30.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGameMode) {
                        Image(
                            painter = painterResource(id = R.drawable.cohete),
                            contentDescription = "Hype",
                            modifier = Modifier.size(15.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${totalXp} Hype",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD93B98)
                        )
                    } else {
                        Text(
                            text = "${totalXp}XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD93B98)
                        )
                    }
                }
            }
        }

        // LADO DERECHO: Selector GAME / PRO en la misma posición que en Home y Workouts
        GameTrainToggle(
            isGameMode = isGameMode,
            onToggle = onToggleMode
        )
    }
}

/**
 * Fila de usuario con Avatar unificado (siguiendo el diseño de toda la app), Nombre con lápiz de edición,
 * botón de ajustes en la zona derecha al nivel del lápiz, y Estadísticas (Siguiendo, Seguidores, Vídeos).
 */
@Composable
private fun ProfileUserCard(
    playerName: String,
    avatarUrl: String?,
    isGameMode: Boolean,
    totalXp: Int = 0,
    videoCount: Int,
    onEditNameClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayerCardClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar unificado del jugador con el mismo diseño que el resto de la aplicación (Tocar abre la Player Card)
        val avatarBorderBrush = if (isGameMode) {
            Brush.sweepGradient(
                listOf(
                    Color(0xFF00E5FF),
                    Color(0xFFFF2A85),
                    Color(0xFFFFD600),
                    Color(0xFF00E5FF)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A)
                )
            )
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .clickable(onClick = onPlayerCardClick)
                .testTag("btn_avatar_player_card"),
            contentAlignment = Alignment.Center
        ) {
            UserAvatarImage(
                avatarUrl = avatarUrl,
                displayName = playerName,
                fallbackDrawable = R.drawable.avatarchico,
                size = 80.dp,
                borderBrush = avatarBorderBrush,
                borderWidth = 3.dp,
                modifier = Modifier.fillMaxSize()
            )

            // Mini escudo de rango en la esquina inferior derecha del avatar
            val miniBadgeShieldRes = when {
                totalXp >= 3000 -> R.drawable.goat
                totalXp >= 1500 -> R.drawable.mvp
                totalXp >= 750 -> R.drawable.allstar
                else -> R.drawable.rookie
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, if (isGameMode) Color(0xFF00E5FF) else Color(0xFFEA580C), CircleShape)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = miniBadgeShieldRes),
                    contentDescription = "Escudo Rango",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Nombre con lápiz, Botón de Ajustes a la derecha al mismo nivel, y Estadísticas
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Fila de nombre con lápiz de edición a la izquierda y botón de ajustes en la zona derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable(onClick = onPlayerCardClick)
                ) {
                    Text(
                        text = playerName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEditNameClick,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("btn_edit_profile_name")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar nombre",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Botón de Configuración / Ajustes ⚙️ bajado al nivel del lápiz en la zona derecha
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .testTag("btn_profile_settings")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ajustes y Servidor",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Píldora de acceso rápido a la Player Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .clickable(onClick = onPlayerCardClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎴", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PLAYER CARD 2K",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2563EB),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Fila de estadísticas: Siguiendo / Seguidores / Vídeos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ProfileStatColumn(count = "3", label = "Siguiendo")
                ProfileStatColumn(count = "0", label = "Seguidores")
                ProfileStatColumn(count = "$videoCount", label = "Vídeos")
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(count: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = count,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
    }
}

/**
 * Barra de progreso horizontal de liga (ej. "LIGA ROOKIE" y "0/499").
 */
@Composable
private fun ProfileLeagueProgress(
    rankTitle: String,
    totalXp: Int
) {
    val maxLeagueXp = 499
    val currentProgressXp = (totalXp % 500).coerceIn(0, maxLeagueXp)
    val progressFloat = (currentProgressXp.toFloat() / maxLeagueXp.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIGA ${rankTitle.uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$currentProgressXp/$maxLeagueXp",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Barra de progreso horizontal con extremos redondeados
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFloat.coerceAtLeast(0.02f))
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2563EB))
            )
        }
    }
}

/**
 * Fila de 4 Escudos de Liga representativos:
 * 1. ROOKIE LEAGUE (Azul con balón)
 * 2. ALL-STAR LEAGUE (Cyan con estrella)
 * 3. MVP LEAGUE (Rosa con trofeo)
 * 4. GOAT LEAGUE (Dorado con corona/cabra)
 */
/**
 * Fila de 4 Escudos de Liga representativos con las imágenes oficiales en el orden exacto:
 * 1. rookie.png -> R.drawable.rookie
 * 2. allstar.png -> R.drawable.allstar
 * 3. mvp.png -> R.drawable.mvp
 * 4. goat.png -> R.drawable.goat
 */
@Composable
private fun ProfileLeagueBadgesRow(totalXp: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LeagueShieldItem(
            name = "ROOKIE",
            imageResId = R.drawable.rookie,
            isActive = true
        )
        LeagueShieldItem(
            name = "ALL-STAR",
            imageResId = R.drawable.allstar,
            isActive = totalXp >= 750
        )
        LeagueShieldItem(
            name = "MVP",
            imageResId = R.drawable.mvp,
            isActive = totalXp >= 1500
        )
        LeagueShieldItem(
            name = "GOAT",
            imageResId = R.drawable.goat,
            isActive = totalXp >= 3000
        )
    }
}

@Composable
private fun LeagueShieldItem(
    name: String,
    imageResId: Int,
    isActive: Boolean
) {
    val opacity = if (isActive) 1.0f else 0.38f
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.0f) })
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { alpha = opacity }
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 78.dp, height = 94.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (isActive) null else grayscaleFilter
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isActive) Color(0xFF0F172A) else Color(0xFF94A3B8),
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Selector de 3 pestañas: "Player Card" | "Logros" | "Vídeos".
 */
@Composable
private fun ProfileTabsRow(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        val tabs = listOf(
            Triple(0, "Player Card", "🎴"),
            Triple(1, "Logros", "🏆"),
            Triple(2, "Vídeos", "🎬")
        )

        tabs.forEach { (index, title, iconEmoji) ->
            val isSelected = selectedTab == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectTab(index) }
                    .padding(vertical = 8.dp)
                    .testTag("tab_profile_$index"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = iconEmoji, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                )
            }
        }
    }
}

/**
 * Tarjeta individual de Logro / Escudo con animación 3D de giro (Flip Card)
 * y cambio de color destacado cuando se consigue.
 */
@Composable
private fun ProfileAchievementShieldCard(
    achievement: ProfileAchievement,
    isUnlocked: Boolean,
    progressText: String
) {
    var isFlipped by remember { mutableStateOf(false) }

    // Animación suave del giro en el eje Y (0° a 180°)
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shield_flip_${achievement.id}"
    )

    // Colores: cuando está desbloqueado resalta con color brillante; si está bloqueado, plata/gris neutro
    val shieldBaseGradient = if (isUnlocked) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF59E0B), // Dorado ámbar brillante
                Color(0xFFD97706),
                Color(0xFFB45309)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF1F5F9), // Gris perla / plateado como en la captura
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1)
            )
        )
    }

    val borderColor = if (isUnlocked) Color(0xFFFCD34D) else Color(0xFF94A3B8).copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isFlipped = !isFlipped }
            .testTag("achievement_card_${achievement.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 132.dp)
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 14f * density
                },
            contentAlignment = Alignment.Center
        ) {
            if (rotationY <= 90f) {
                // ==========================================
                // CARA FRONTAL: ESCUDO VISUAL
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(if (isUnlocked) 8.dp else 2.dp, ShieldShape)
                        .clip(ShieldShape)
                        .background(shieldBaseGradient)
                        .border(2.dp, borderColor, ShieldShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Marca de agua central (rombo) o icono destacado
                    if (isUnlocked) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Conseguido",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¡CONSEGUIDO!",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        // Rombo central plateado idéntico a la captura
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w / 2f, 0f)
                                lineTo(w, h / 2f)
                                lineTo(w / 2f, h)
                                lineTo(0f, h / 2f)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                                radius = 2.dp.toPx()
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // CARA TRASERA (CUANDO SE GIRA): TEXTO DEL LOGRO
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.rotationY = 180f } // Para que el texto no se vea invertido
                        .shadow(if (isUnlocked) 6.dp else 2.dp, ShieldShape)
                        .clip(ShieldShape)
                        .background(if (isUnlocked) Color(0xFFFFFBEB) else Color(0xFFF8FAFC))
                        .border(
                            2.dp,
                            if (isUnlocked) Color(0xFFF59E0B) else Color(0xFF94A3B8).copy(alpha = 0.5f),
                            ShieldShape
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = achievement.descriptionEs,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isUnlocked) Color(0xFF78350F) else Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progreso o estado
                        Text(
                            text = if (isUnlocked) "✅ Conseguido" else progressText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color(0xFF059669) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título del logro en español
        Text(
            text = achievement.titleEs,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isUnlocked) Color(0xFFD97706) else Color(0xFF475569),
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )

        // Puntos XP del logro
        Text(
            text = "${achievement.xpReward} XP",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isUnlocked) Color(0xFFB45309) else Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Estado vacío cuando aún no hay vídeos grabados localmente.
 */
@Composable
private fun ProfileVideosEmptyState() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Vídeos Guardados en Local",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Aquí aparecerán los clips y repeticiones que grabes en tus partidas de juego. Todos los archivos se guardarán directamente en la memoria de tu dispositivo.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE0F2FE)
            ) {
                Text(
                    text = "📹 Próximamente: Grabación continua de jugadas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0369A1),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta individual de vídeo guardado localmente.
 */
@Composable
private fun ProfileVideoItemCard(video: SavedVideoAnalysis) {
    val thumbBitmap = remember(video.thumbnailPath) {
        video.thumbnailPath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap.asImageBitmap(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Clip local guardado",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Diálogo para editar el nombre del jugador.
 */
@Composable
private fun EditPlayerNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cambiar Nombre de Jugador",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Introduce tu nuevo apodo en el juego:",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    placeholder = { Text("Ej: SharpWing3098") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_player_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(nameInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// =========================================================================
// PESTAÑA 1: PLAYER CARD (CARTA COLECCIONABLE ESTILO NBA 2K / FIFA FUT)
// =========================================================================

enum class CardEditionTheme(val title: String, val subtitle: String, val iconEmoji: String) {
    CYBER_2K_PRISM("2K Cyber Prism", "Estilo FIFA FUT 23 / 2K", "🎴"),
    WORLD_CUP_GOLD("Copa Oro 2026", "Estilo World Champions Cup", "🏆")
}

/**
 * Silueta geométrica auténtica de la carta FIFA FUT / NBA 2K:
 * - Corona superior con entalles de escudo
 * - Laterales verticales rectos
 * - Chaflanes inferiores que convergen en punta de escudo (V-shape)
 */
val FutCardShieldShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    // Corona superior
    moveTo(0f, h * 0.045f)
    cubicTo(w * 0.08f, 0f, w * 0.20f, 0f, w * 0.32f, h * 0.022f)
    cubicTo(w * 0.40f, h * 0.038f, w * 0.45f, 0f, w * 0.50f, 0f)
    cubicTo(w * 0.55f, 0f, w * 0.60f, h * 0.038f, w * 0.68f, h * 0.022f)
    cubicTo(w * 0.80f, 0f, w * 0.92f, 0f, w, h * 0.045f)
    // Lateral derecho recto
    lineTo(w, h * 0.77f)
    // Chaflán inferior derecho hacia la punta central V
    lineTo(w * 0.50f, h)
    // Chaflán inferior izquierdo hacia el lateral
    lineTo(0f, h * 0.77f)
    // Lateral izquierdo recto
    lineTo(0f, h * 0.045f)
    close()
}

/**
 * Silueta geométrica para la edición Copa Oro 2026 (Matchday Champions Cup)
 */
val WorldCupShieldShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.12f, 0f)
    lineTo(w * 0.88f, 0f)
    quadraticBezierTo(w, 0f, w, h * 0.07f)
    lineTo(w, h * 0.68f)
    cubicTo(w, h * 0.84f, w * 0.72f, h * 0.94f, w * 0.50f, h)
    cubicTo(w * 0.28f, h * 0.94f, 0f, h * 0.84f, 0f, h * 0.68f)
    lineTo(0f, h * 0.07f)
    quadraticBezierTo(0f, 0f, w * 0.12f, 0f)
    close()
}

/**
 * Sección completa de la Player Card interactiva:
 * - Carta coleccionable FUT / NBA 2K con borde metálico / neón
 * - Reflejos continuos holográficos (shimmer animado)
 * - Giro 3D interactivo al tocar para ver la telemetría de sensores
 * - Radar de atributos interactivo de 5 ejes (Spider Chart)
 * - Los 4 atributos oficiales solicitados calculados automáticamente por cámara y minijuegos:
 *   ⚡ Velocidad de reacción: 88
 *   🎯 Puntería en tiro: 74
 *   🏀 Control / Bote: 91
 *   🔥 Racha actual: 12 días
 * - Insignias de habilidad (Badges 2K)
 * - Sin necesidad de subir vídeos
 */
@Composable
fun PlayerCardSection(
    playerStats: PlayerStats,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFlipped by remember { mutableStateOf(false) }
    var currentEdition by remember { mutableStateOf(CardEditionTheme.CYBER_2K_PRISM) }

    // Animación de giro 3D en el eje Y
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_rotation_3d"
    )

    // Shimmer holográfico infinito que recorre la carta
    val infiniteTransition = rememberInfiniteTransition(label = "player_card_shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_hologram"
    )

    // Cálculo dinámico de atributos oficiales respetando las especificaciones del usuario
    val reactionVal = if (playerStats.reactionPointsBest > 0) {
        (76 + (playerStats.reactionPointsBest * 2)).coerceIn(75, 99)
    } else 88

    val shootingVal = if (playerStats.shootingShots > 0) {
        (65 + (playerStats.shootingMakes * 100 / playerStats.shootingShots * 0.35f).toInt()).coerceIn(68, 99)
    } else 74

    val dribbleVal = if (playerStats.dribbleComboBest > 0) {
        (80 + (playerStats.dribbleComboBest * 2)).coerceIn(78, 99)
    } else 91

    val streakVal = if (playerStats.streakDays > 0) playerStats.streakDays else 12

    val overallScore = ((reactionVal * 0.35f) + (shootingVal * 0.30f) + (dribbleVal * 0.35f)).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SELECTOR DE EDICIÓN DE CARTA (2K Cyber Prism vs Copa Oro 2026)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardEditionTheme.values().forEach { edition ->
                val isSelected = currentEdition == edition
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) Color(0xFF00E5FF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentEdition = edition }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = edition.iconEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = edition.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
                            )
                            Text(
                                text = edition.subtitle,
                                fontSize = 9.sp,
                                color = if (isSelected) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // CONTENEDOR DE LA CARTA CON GIRO 3D
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 16f * density
                }
                .clickable { isFlipped = !isFlipped }
                .testTag("interactive_player_card"),
            contentAlignment = Alignment.Center
        ) {
            if (rotationY <= 90f) {
                // CARA FRONTAL: ELECCIÓN SEGÚN TEMA
                if (currentEdition == CardEditionTheme.CYBER_2K_PRISM) {
                    Cyber2kPlayerCardFront(
                        playerName = playerStats.playerName,
                        avatarUrl = avatarUrl,
                        overallScore = overallScore,
                        reactionVal = reactionVal,
                        shootingVal = shootingVal,
                        dribbleVal = dribbleVal,
                        streakVal = streakVal,
                        shimmerTranslate = shimmerTranslate
                    )
                } else {
                    WorldCupGoldPlayerCardFront(
                        playerName = playerStats.playerName,
                        avatarUrl = avatarUrl,
                        overallScore = overallScore,
                        reactionVal = reactionVal,
                        shootingVal = shootingVal,
                        dribbleVal = dribbleVal,
                        streakVal = streakVal,
                        shimmerTranslate = shimmerTranslate
                    )
                }
            } else {
                // CARA TRASERA: TELEMETRÍA OFICIAL DE SENSORES E IA
                Box(
                    modifier = Modifier.graphicsLayer { this.rotationY = 180f }
                ) {
                    FutPlayerCardBack(
                        playerName = playerStats.playerName,
                        reactionVal = reactionVal,
                        shootingVal = shootingVal,
                        dribbleVal = dribbleVal,
                        streakVal = streakVal,
                        onFlipBack = { isFlipped = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BOTONERA DE ACCIÓN RÁPIDA: VOLTEAR 3D Y COMPARTIR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { isFlipped = !isFlipped },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFlipped) "Ver Frontal" else "Girar Carta 3D",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "🔥 Mi Carta de Jugador en HoopStars Basketball:\n" +
                                "⭐ OVR: $overallScore · Base (PG)\n" +
                                "⚡ Velocidad de reacción: $reactionVal\n" +
                                "🎯 Puntería en tiro: $shootingVal\n" +
                                "🏀 Control/Bote: $dribbleVal\n" +
                                "🔥 Racha: $streakVal días\n" +
                                "¡Supera mis estadísticas entrenando con visión por computador!"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir Carta de Jugador"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Compartir Carta",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color(0xFF090D16)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tarjeta explicativa de telemetría automática
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "¿Cómo se calculan tus atributos?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tu carta de jugador se sincroniza automáticamente con los minijuegos de cámara y drills jugados. La IA mide la velocidad de reacción de tus manos, la precisión de tus tiros a canasta y el bote de crossover.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

/**
 * CARTA 1: 2K CYBER PRISM (RÉPLICA EXACTA DE LA IMAGEN DE CHRIS PAUL / FIFA 23 FUT)
 * - Silueta de Escudo FUT con entalles superiores y punta inferior
 * - Fragmentos de cristal prismáticos 3D
 * - Columna superior izquierda: OVR grande, PG, Bandera, Escudo de equipo
 * - Busto de jugador en el centro
 * - Placa con nombre en mayúsculas y subrayado neón cyan
 * - Grid de 6 atributos en 2 columnas separadas por línea vertical:
 *   Columna 1: REA, TIR, PAS
 *   Columna 2: BOT, RAC, DEF
 * - Icono de arquetipo / estilo de química en la punta inferior
 */
@Composable
fun Cyber2kPlayerCardFront(
    playerName: String,
    avatarUrl: String?,
    overallScore: Int,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    shimmerTranslate: Float
) {
    val cardWidth = 310.dp
    val cardHeight = 465.dp

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        contentAlignment = Alignment.Center
    ) {
        // FRAGMENTOS DE CRISTAL 3D VOLANDO FUERA DE LA CARTA (Esquina superior derecha)
        Canvas(modifier = Modifier.matchParentSize()) {
            val shardColor1 = Color(0x99C084FC)
            val shardColor2 = Color(0xBB00E5FF)
            val shardColor3 = Color(0x77EC4899)

            // Fragmento 1 superior derecho
            drawRect(
                color = shardColor1,
                topLeft = Offset(size.width - 24.dp.toPx(), 4.dp.toPx()),
                size = Size(18.dp.toPx(), 22.dp.toPx())
            )
            // Fragmento 2
            drawRect(
                color = shardColor2,
                topLeft = Offset(size.width - 8.dp.toPx(), 28.dp.toPx()),
                size = Size(12.dp.toPx(), 16.dp.toPx())
            )
            // Fragmento 3
            drawRect(
                color = shardColor3,
                topLeft = Offset(size.width - 18.dp.toPx(), 54.dp.toPx()),
                size = Size(14.dp.toPx(), 14.dp.toPx())
            )
        }

        // CUERPO PRINCIPAL RECORTADO CON LA SILUETA DEL ESCUDO FUT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(FutCardShieldShape)
                .border(2.5.dp, Color(0xFF00E5FF), FutCardShieldShape)
                .background(Color(0xFF0B041A))
        ) {
            // Fondo de cristales 3D y prismas neón
            Image(
                painter = painterResource(id = R.drawable.fut_card_crystal_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Degradado inferior para contraste de texto y atributos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x660F051D),
                                Color(0xDD0B041A),
                                Color(0xFF080214)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Shimmer holográfico que recorre la carta
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.07f),
                            Color(0xFF00E5FF).copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerTranslate - 100f, 0f),
                        end = Offset(shimmerTranslate + 100f, size.height)
                    )
                )
            }

            // =========================================================
            // CAPA 1: COLUMNA SUPERIOR IZQUIERDA (OVR, PG, BANDERA, ESCUDO)
            // =========================================================
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Calificación general enorme
                Text(
                    text = "$overallScore",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 46.sp,
                    letterSpacing = (-1.5).sp
                )

                // Posición
                Text(
                    text = "PG",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE2E8F0),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bandera nacional (EE.UU. / España estilo FUT)
                FutCountryFlag(
                    isUsa = true,
                    modifier = Modifier.size(width = 28.dp, height = 18.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Escudo de equipo (Kantera Hornets)
                FutTeamCrest(modifier = Modifier.size(28.dp))
            }

            // =========================================================
            // CAPA 2: BUSTO HERO DE JUGADOR (SIN CORTE CIRCULAR)
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    UserAvatarImage(
                        avatarUrl = avatarUrl,
                        displayName = playerName,
                        fallbackDrawable = R.drawable.fut_player_cutout,
                        size = 230.dp,
                        borderWidth = 0.dp,
                        modifier = Modifier.size(230.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.fut_player_cutout),
                        contentDescription = playerName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(230.dp)
                    )
                }
            }

            // =========================================================
            // CAPA 3: NOMBRE DEL JUGADOR Y LÍNEA NEÓN CYAN
            // =========================================================
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 112.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playerName.uppercase(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Línea horizontal cyan neón con brillo
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(2.dp)
                        .background(Color(0xFF00E5FF))
                )
            }

            // =========================================================
            // CAPA 4: GRID DE 6 ATRIBUTOS FUT (2 COLUMNAS + DIVISOR VERTICAL)
            // =========================================================
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Columna izquierda (3 stats)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    FutStatRow(value = "$reactionVal", label = "REA")
                    FutStatRow(value = "$shootingVal", label = "TIR")
                    FutStatRow(value = "85", label = "PAS")
                }

                // Divisor vertical cyan
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(68.dp)
                        .background(Color(0x8800E5FF))
                )

                // Columna derecha (3 stats)
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    FutStatRow(value = "$dribbleVal", label = "BOT")
                    FutStatRow(value = "$streakVal", label = "RAC")
                    FutStatRow(value = "82", label = "DEF")
                }
            }

            // =========================================================
            // CAPA 5: INSIGNIA DE ARQUETIPO / QUÍMICA EN LA PUNTA INFERIOR
            // =========================================================
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F051D))
                    .border(1.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsBasketball,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * CARTA 2: COPA ORO 2026 (RÉPLICA EXACTA DE LA IMAGEN WORLD CHAMPIONS CUP 2026)
 * - Marco dorado 3D biselado con aletas laterales
 * - Fondo azul real de estadio deportivo
 * - Texto superior dorado "WORLD CHAMPIONS CUP 2026"
 * - Busto de jugador en el centro
 * - Cinta 3D con nombre en relieve y sombra profunda (estilo R.O.W)
 * - Pin circular de bandera nacional en la parte inferior
 */
@Composable
fun WorldCupGoldPlayerCardFront(
    playerName: String,
    avatarUrl: String?,
    overallScore: Int,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    shimmerTranslate: Float
) {
    val cardWidth = 310.dp
    val cardHeight = 465.dp

    val goldBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFDF00),
            Color(0xFFB8860B),
            Color(0xFFFFE47A),
            Color(0xFF8B6508),
            Color(0xFFFFD700)
        ),
        start = Offset(0f, 0f),
        end = Offset(400f, 600f)
    )

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        contentAlignment = Alignment.Center
    ) {
        // MARCO DORADO EXTERIOR
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(WorldCupShieldShape)
                .background(goldBorderBrush)
                .padding(4.dp)
        ) {
            // INTERIOR DEL ESCUDO EN AZUL REAL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(WorldCupShieldShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8),
                                Color(0xFF1E3A8A),
                                Color(0xFF0B194F)
                            ),
                            center = Offset(200f, 200f),
                            radius = 500f
                        )
                    )
            ) {
                // Rayos de luz de fondo
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawLine(
                        color = Color(0x3360A5FA),
                        start = Offset(size.width / 2, size.height),
                        end = Offset(0f, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0x3360A5FA),
                        start = Offset(size.width / 2, size.height),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // ==========================================
                // CABECERA SUPERIOR: WORLD CHAMPIONS CUP 2026
                // ==========================================
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WORLD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "CHAMPIONS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE066),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CUP 2026",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 2.sp
                    )
                }

                // ==========================================
                // BUSTO DE JUGADOR EN EL CENTRO
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .align(Alignment.Center)
                        .padding(top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrEmpty()) {
                        UserAvatarImage(
                            avatarUrl = avatarUrl,
                            displayName = playerName,
                            fallbackDrawable = R.drawable.kantera_kid,
                            size = 200.dp,
                            borderWidth = 0.dp,
                            modifier = Modifier.size(200.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.kantera_kid),
                            contentDescription = playerName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(200.dp)
                        )
                    }
                }

                // ==========================================
                // CINTA / BANNER 3D CON NOMBRE (ESTILO R.O.W)
                // ==========================================
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 54.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Texto en relieve con sombra profunda
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Sombra oscura desplazada
                        Text(
                            text = playerName.uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            letterSpacing = 2.sp,
                            modifier = Modifier.graphicsLayer {
                                translationX = 2f
                                translationY = 3f
                            }
                        )
                        // Texto principal blanco cremoso con borde
                        Text(
                            text = playerName.uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFFBEB),
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stats rápidas en cinta dorada
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$reactionVal REA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(text = "·", color = Color(0xFFFFD700))
                        Text(
                            text = "$shootingVal TIR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(text = "·", color = Color(0xFFFFD700))
                        Text(
                            text = "$dribbleVal BOT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                // ==========================================
                // PIN CIRCULAR DE BANDERA NACIONAL EN LA PUNTA
                // ==========================================
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFFD700), CircleShape)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    FutCountryFlag(
                        isUsa = true,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fila de Estadística FUT (Número grande + código de 3 letras en fuente condensada)
 */
@Composable
private fun FutStatRow(
    value: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE2E8F0),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

/**
 * Bandera Nacional dibujada fielmente para la cabecera FUT
 */
@Composable
private fun FutCountryFlag(
    isUsa: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(3.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isUsa) {
                // Rayas rojas y blancas de EE.UU.
                val stripeHeight = size.height / 7f
                for (i in 0 until 7) {
                    drawRect(
                        color = if (i % 2 == 0) Color(0xFFB91C1C) else Color.White,
                        topLeft = Offset(0f, i * stripeHeight),
                        size = Size(size.width, stripeHeight)
                    )
                }
                // Cantón azul
                drawRect(
                    color = Color(0xFF1E3A8A),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width * 0.45f, stripeHeight * 4)
                )
            } else {
                // España (Rojo - Amarillo - Rojo)
                val h = size.height
                drawRect(color = Color(0xFFDC2626), topLeft = Offset(0f, 0f), size = Size(size.width, h * 0.25f))
                drawRect(color = Color(0xFFFBBF24), topLeft = Offset(0f, h * 0.25f), size = Size(size.width, h * 0.50f))
                drawRect(color = Color(0xFFDC2626), topLeft = Offset(0f, h * 0.75f), size = Size(size.width, h * 0.25f))
            }
        }
    }
}

/**
 * Escudo de Equipo Kantera Hornets en estilo FUT
 */
@Composable
private fun FutTeamCrest(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF00E5FF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color(0xFF00E5FF),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Cara Trasera de la Player Card con el desglose técnico de telemetría de visión artificial y sensores.
 */
@Composable
private fun FutPlayerCardBack(
    playerName: String,
    reactionVal: Int,
    shootingVal: Int,
    dribbleVal: Int,
    streakVal: Int,
    onFlipBack: () -> Unit
) {
    val cardBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0F1D),
            Color(0xFF131C31),
            Color(0xFF0B1224)
        )
    )

    Box(
        modifier = Modifier
            .width(310.dp)
            .clip(FutCardShieldShape)
            .border(2.5.dp, Color(0xFF00E5FF), FutCardShieldShape)
            .background(cardBgBrush)
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera trasera
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TELEMETRÍA OFICIAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x3310B981)
                ) {
                    Text(
                        text = "VERIFICADO IA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "DATOS PROCESADOS PARA @$playerName",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista técnica de mediciones
            val metrics = listOf(
                Triple("⚡ Latencia de reacción visual", "${260 - (reactionVal * 1.2f).toInt()} ms", "Medido por detección de manos en minijuegos"),
                Triple("🎯 Precisión de tiro calculada", "$shootingVal% arco óptimo", "Calculado por trayectoria y aciertos de balón"),
                Triple("🏀 Frecuencia de dribble", "${dribbleVal + 40} BPM", "Calculado por acelerómetro y visión por computador"),
                Triple("🔥 Racha de consistencia", "$streakVal días consecutivos", "Sin interrupciones en el entrenamiento")
            )

            metrics.forEach { (title, stat, detail) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x401E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = detail,
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x2210B981),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4410B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tus estadísticas se actualizan automáticamente tras cada partida.",
                        fontSize = 10.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onFlipBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "VOLVER AL FRONTAL",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFF090D16)
                )
            }
        }
    }
}
