package com.example.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import com.example.ui.common.UserAvatarImage
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Person
import java.util.Locale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

enum class HomeBottomTab {
    HOME,
    THE_COURT,
    WORKOUT,
    PROFILE
}

data class HeroWorkoutSlide(
    val title: String,
    val subtitle: String = "Finish 5 more workouts to unlock Level 2!",
    val drillName: String,
    val duration: String = "2 MIN",
    val difficulty: String = "BEGINNER",
    val progressText: String = "0%",
    val isLocked: Boolean = false,
    val requiredXp: Int = 0,
    val currentXp: Int = 32,
    val levelNumber: Int = 1,
    val imageResId: Int = R.drawable.point,
    val onAction: () -> Unit
)

/**
 * Pantalla principal Home inspirada fielmente en la imagen de referencia:
 * - Header: WELCOME BACK SHARPWING3098! + Avatar H★ multicolor
 * - Banner incentivo: 🎉 Keep training and climbing the leaderboard!
 * - Tarjeta Hero convertida en Slide Horizontal (HorizontalPager) con diseño azul eléctrico,
 *   medidor 0%, botón Play negro y todas usando por el momento la imagen point.png.
 * - Fila con las 3 tarjetas de gamificación (ROOKIE, 0 DAYS, 0 XP) con badges flotantes.
 * - Barra inferior oscura con acceso a The Court, Workout, Profile y Home con botón elevado.
 */
@Composable
fun MainHomeScreen(
    onNavigateToWorkouts: () -> Unit,
    onLaunchDefendZoneDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchDribbleDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchReactionPointsDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchShootingDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchKidsMiniBasketDrill: () -> Unit = onNavigateToWorkouts,
    userHandle: String = "SHARPWING3098",
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(HomeBottomTab.HOME) }
    var isGameMode by rememberSaveable { mutableStateOf(true) }
    var showXpAdPopup by remember { mutableStateOf(false) }
    var showProInfoDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showXpBreakdownDialog by remember { mutableStateOf(false) }
    var showStreakInfoDialog by remember { mutableStateOf(false) }
    val currentUser by com.example.supabase.SupabaseAuthManager.currentUser.collectAsState()
    val playerStats by com.example.stats.PlayerStatsManager.stats.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Comprobación y avance de la racha diaria al ingresar a la app
    LaunchedEffect(Unit) {
        com.example.stats.PlayerStatsManager.checkAndUpdateStreak()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "home_pulse_anim")
    val playPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "play_pulse"
    )
    val flameGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_glow"
    )

    // Slides del Hero ordenados según preferencia con XP y niveles dinámicos reales:
    // Lista ordenada de entrenamientos y juegos del carrusel:
    // 1. REACTION POINTS (Ball & Touch): R.drawable.kantera_point
    // 2. REACTION POINTS (Cross Touch): R.drawable.kantera_point2
    // 3. DEFEND THE ZONE (DEFEND ZONE): R.drawable.kantera_mano
    // 4. DEFEND THE ZONE COPIA (LASER ZONE): R.drawable.kantera_laser
    // 5. SHOOTING: R.drawable.kantera_tiro
    // 6. KIDS MINI BASKET: R.drawable.kantera_kid
    // 7. DRIBBLING: R.drawable.img_drill_dribbling
    // 8, 9, 10. Tarjetas de ejemplos bloqueadas con progreso de XP
    val heroSlides = remember(
        playerStats.totalXp,
        onLaunchDefendZoneDrill,
        onLaunchDribbleDrill,
        onLaunchReactionPointsDrill,
        onLaunchShootingDrill,
        onLaunchKidsMiniBasketDrill
    ) {
        listOf(
            // 1. REACTION POINTS (Ball & Touch)
            HeroWorkoutSlide(
                title = "NEXT UP: DRIBBLE RUSH",
                subtitle = "Toca los objetivos luminosos mientras mantienes el bote",
                drillName = "Ball & Touch",
                duration = "2 MIN",
                difficulty = "BEGINNER",
                isLocked = false,
                requiredXp = 0,
                currentXp = playerStats.totalXp,
                levelNumber = 1,
                imageResId = R.drawable.kantera_point,
                onAction = onLaunchReactionPointsDrill
            ),
            // 2. REACTION POINTS (Cross Touch)
            HeroWorkoutSlide(
                title = "NEXT UP: DRIBBLE RUSH",
                subtitle = "Toca los objetivos luminosos mientras mantienes el bote",
                drillName = "Cross Touch",
                duration = "2 MIN",
                difficulty = "BEGINNER",
                isLocked = false,
                requiredXp = 0,
                currentXp = playerStats.totalXp,
                levelNumber = 2,
                imageResId = R.drawable.kantera_point2,
                onAction = onLaunchReactionPointsDrill
            ),
            // 3. DEFEND THE ZONE (DEFEND ZONE)
            HeroWorkoutSlide(
                title = "¡NUEVO! DEFEND THE ZONE",
                subtitle = "Esquiva los defensores fantasma y protege tu bote (3 vidas)",
                drillName = "DEFEND ZONE",
                duration = "1 MIN",
                difficulty = "PRO",
                isLocked = false,
                requiredXp = 0,
                currentXp = playerStats.totalXp,
                levelNumber = 3,
                imageResId = R.drawable.kantera_mano,
                onAction = onLaunchDefendZoneDrill
            ),
            // 4. DEFEND THE ZONE (LASER ZONE)
            HeroWorkoutSlide(
                title = "¡NUEVO! DEFEND THE ZONE",
                subtitle = "Esquiva los defensores fantasma y protege tu bote (3 vidas)",
                drillName = "LASER ZONE",
                duration = "1 MIN",
                difficulty = "PRO",
                isLocked = false,
                requiredXp = 0,
                currentXp = playerStats.totalXp,
                levelNumber = 4,
                imageResId = R.drawable.kantera_laser,
                onAction = onLaunchDefendZoneDrill
            ),
            // 5. SHOOTING (ANÁLISIS DE TIRO)
            HeroWorkoutSlide(
                title = "NEXT UP: SHOOTING FORM",
                subtitle = "Perfecciona tu mecánica, arco y aciertos",
                drillName = "SHOOTING",
                duration = "3 MIN",
                difficulty = "BEGINNER",
                isLocked = false,
                requiredXp = 0,
                currentXp = playerStats.totalXp,
                levelNumber = 5,
                imageResId = R.drawable.kantera_tiro,
                onAction = onLaunchShootingDrill
            ),
            // 6. KIDS MINI BASKET
            HeroWorkoutSlide(
                title = "¡NUEVO! KIDS MINI BASKET",
                subtitle = "Tiro infantil en casa con calibración de aro y pelota por foto",
                drillName = "MINI BASKET",
                duration = "1 MIN",
                difficulty = "KIDS / CASA",
                isLocked = false,
                requiredXp = 0,
                currentXp = playerStats.totalXp,
                levelNumber = 6,
                imageResId = R.drawable.kantera_kid,
                onAction = onLaunchKidsMiniBasketDrill
            ),
            // 7. DRIBBLING COMBO
            HeroWorkoutSlide(
                title = "NEXT UP: DRIBBLING COMBO",
                subtitle = "Entrena tu control y precisión de bote",
                drillName = "DRIBBLING",
                duration = "2 MIN",
                difficulty = "BEGINNER",
                isLocked = false,
                requiredXp = 0,
                currentXp = playerStats.totalXp,
                levelNumber = 7,
                imageResId = R.drawable.img_drill_dribbling,
                onAction = onLaunchDribbleDrill
            ),
            // 8. EJEMPLO BLOQUEADO 1: CROSSOVER TARGETS
            HeroWorkoutSlide(
                title = "CROSSOVER TARGETS",
                subtitle = "Sube de nivel para desbloquear este entrenamiento",
                drillName = "CROSSOVER",
                duration = "2 MIN",
                difficulty = "LEVEL 2",
                isLocked = playerStats.totalXp < 500,
                requiredXp = 500,
                currentXp = playerStats.totalXp,
                levelNumber = 8,
                imageResId = R.drawable.kantera_manita,
                onAction = { }
            ),
            // 9. EJEMPLO BLOQUEADO 2: STEP-BACK SHOOTING
            HeroWorkoutSlide(
                title = "STEP-BACK SHOOTING",
                subtitle = "Sube de nivel para desbloquear este entrenamiento",
                drillName = "STEP-BACK",
                duration = "3 MIN",
                difficulty = "LEVEL 3",
                isLocked = playerStats.totalXp < 750,
                requiredXp = 750,
                currentXp = playerStats.totalXp,
                levelNumber = 9,
                imageResId = R.drawable.img_onboarding_player,
                onAction = { }
            ),
            // 10. EJEMPLO BLOQUEADO 3: PRO AGILITY & ATTACK
            HeroWorkoutSlide(
                title = "PRO AGILITY & ATTACK",
                subtitle = "Sube de nivel para desbloquear este entrenamiento",
                drillName = "AGILITY PRO",
                duration = "4 MIN",
                difficulty = "ALL-STAR",
                isLocked = playerStats.totalXp < 1000,
                requiredXp = 1000,
                currentXp = playerStats.totalXp,
                levelNumber = 10,
                imageResId = R.drawable.avatarchico,
                onAction = { }
            )
        )
    }

    val greetingName = currentUser?.username?.ifBlank { playerStats.playerName } ?: playerStats.playerName
    val headerAvatarUrl = currentUser?.avatarUrl?.ifBlank { playerStats.avatarUrl } ?: playerStats.avatarUrl

    BackHandler(enabled = selectedTab != HomeBottomTab.HOME) {
        selectedTab = HomeBottomTab.HOME
    }

    val backgroundColor = if (isGameMode) Color.White else Color(0xFF121212)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Contenido principal según la pestaña activa
        if (selectedTab == HomeBottomTab.PROFILE) {
            com.example.profile.FullProfileScreen(
                onOpenSettings = { showProfileDialog = true },
                isGameMode = isGameMode,
                onToggleMode = { newMode ->
                    val switchingToPro = isGameMode && !newMode
                    isGameMode = newMode
                    if (switchingToPro) {
                        showProInfoDialog = true
                    }
                },
                avatarUrl = headerAvatarUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            )
        } else if (selectedTab == HomeBottomTab.WORKOUT) {
            WorkoutCatalogScreen(
                slides = heroSlides,
                isGameMode = isGameMode,
                playerName = greetingName,
                avatarUrl = headerAvatarUrl,
                playerStats = playerStats,
                onBackToHome = { selectedTab = HomeBottomTab.HOME },
                onProfileClick = { selectedTab = HomeBottomTab.PROFILE },
                onOpenXpBreakdown = { showXpBreakdownDialog = true },
                onToggleMode = { newMode ->
                    val switchingToPro = isGameMode && !newMode
                    isGameMode = newMode
                    if (switchingToPro) {
                        showProInfoDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            )
        } else if (selectedTab == HomeBottomTab.THE_COURT) {
            if (isGameMode) {
                BattleGameScreen(
                    playerName = greetingName,
                    avatarUrl = headerAvatarUrl,
                    isGameMode = isGameMode,
                    playerStats = playerStats,
                    onToggleMode = { newMode ->
                        val switchingToPro = isGameMode && !newMode
                        isGameMode = newMode
                        if (switchingToPro) {
                            showProInfoDialog = true
                        }
                    },
                    onProfileClick = { selectedTab = HomeBottomTab.PROFILE },
                    onOpenXpBreakdown = { showXpBreakdownDialog = true },
                    onBackToHome = { selectedTab = HomeBottomTab.HOME },
                    onLaunchReactionPoints = onLaunchReactionPointsDrill,
                    onLaunchDefendZone = onLaunchDefendZoneDrill,
                    onLaunchShooting = onLaunchShootingDrill,
                    onLaunchKidsBasket = onLaunchKidsMiniBasketDrill,
                    onLaunchDribble = onLaunchDribbleDrill,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            } else {
                ProStatsScreen(
                    playerStats = playerStats,
                    onBackToHome = { selectedTab = HomeBottomTab.HOME },
                    onLaunchShootingDrill = onLaunchShootingDrill,
                    onLaunchReactionDrill = onLaunchReactionPointsDrill,
                    onLaunchDefendDrill = onLaunchDefendZoneDrill,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        } else {
            // Contenido del Home con cabecera fija de altura y posición idéntica a Workout y Perfil
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 90.dp)
            ) {
                // 1. TOP HEADER (Avatar a la izquierda + "Hola Miguel" + Toggle GAME / PRO a la derecha)
                HomeHeaderRow(
                    userGreeting = "Hola $greetingName",
                    avatarUrl = headerAvatarUrl,
                    isGameMode = isGameMode,
                    onToggleMode = { newMode ->
                        val switchingToPro = isGameMode && !newMode
                        isGameMode = newMode
                        if (switchingToPro) {
                            showProInfoDialog = true
                        }
                    },
                    onProfileClick = { selectedTab = HomeBottomTab.PROFILE },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(44.dp)
                )

                // 2. Contenido scrolleable del Home
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {

                // 2. BANNER PUBLICITARIO / PROMOCIONAL
                // Oculto a petición del usuario (se conserva intacto para reactivar cuando se solicite)
                val showTopAdBanner = false
                if (showTopAdBanner) {
                    HomeImageBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3. TARJETA HERO EN FORMATO SLIDE HORIZONTAL
                HeroCardsSlider(
                    slides = heroSlides,
                    isGameMode = isGameMode,
                    onTransitionToCard = { fromIndex, toIndex ->
                        // Si pasa de la tarjeta 4 (índice 3) a la tarjeta 5 (índice 4)
                        if (fromIndex == 3 && toIndex == 4) {
                            showXpAdPopup = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // En modo GAME se muestran las métricas de gamificación y leaderboard.
                // En modo TRAIN (profesional) se muestran los contadores de tiros y entrenamientos reales guardados.
                if (isGameMode) {
                    Spacer(modifier = Modifier.height(26.dp))

                    // 4. FILA DE GAMIFICACIÓN: ROOKIE (Magenta), DIAS (Cyan), XP (Azul)
                    GamificationCardsRow(
                        flameGlowAlpha = flameGlowAlpha,
                        rankTitle = playerStats.rankTitle,
                        level = playerStats.level,
                        streakDays = playerStats.streakDays,
                        totalXp = playerStats.totalXp,
                        onRankClick = { showXpBreakdownDialog = true },
                        onStreakClick = { showStreakInfoDialog = true },
                        onXpClick = { showXpBreakdownDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 5. SECCIÓN LEADERBOARD (Podium Top 3, Jugadores Cercanos, Boost Hype)
                    val activeAvatarUrl = currentUser?.avatarUrl?.ifBlank { playerStats.avatarUrl } ?: playerStats.avatarUrl
                    LeaderboardSection(
                        userHandle = greetingName,
                        userXp = "${playerStats.totalXp} Hype",
                        userAvatarUrl = activeAvatarUrl,
                        onViewMoreClick = { selectedTab = HomeBottomTab.WORKOUT },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 6. TICKER DE HITOS EN VIVO (Live Activity Stream estilo Apple Fitness / Marcador NBA)
                    LiveMilestonesTicker(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. MODO PRO: CONTADORES OFICIALES DE TIROS Y ENTRENAMIENTOS REALES GUARDADOS
                    ProDashboardStatsSection(
                        playerStats = playerStats,
                        onNavigateToWorkouts = { selectedTab = HomeBottomTab.WORKOUT },
                        onOpenXpBreakdown = { showXpBreakdownDialog = true },
                        onStreakClick = { showStreakInfoDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // 5. BARRA DE NAVEGACIÓN INFERIOR (#2FB2C9 en GAME, negro #111114 en TRAIN, con textos e iconos en blanco)
        HomeBottomNavigationBar(
            selectedTab = selectedTab,
            isGameMode = isGameMode,
            onTabSelected = { tab ->
                if (tab == HomeBottomTab.HOME) {
                    if (selectedTab == HomeBottomTab.HOME) {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    }
                }
                selectedTab = tab
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Popup de anuncio para ganar más XP (al pasar de la tarjeta 2 a la 3)
        if (showXpAdPopup) {
            XpAdRewardPopup(
                onDismiss = { showXpAdPopup = false },
                onWatchAdClick = {
                    // Acción fake: no hace nada y cierra el popup
                    showXpAdPopup = false
                }
            )
        }

        // Popup informativo del MODO PRO al activarlo
        if (showProInfoDialog) {
            ProModeInfoDialog(
                onDismiss = { showProInfoDialog = false }
            )
        }

        // Diálogo de Perfil y Sincronización Supabase Cloud (accesible desde el engranaje de ajustes)
        if (showProfileDialog) {
            com.example.supabase.ProfileCloudSyncDialog(
                isGameMode = isGameMode,
                onDismiss = {
                    showProfileDialog = false
                }
            )
        }

        // Diálogo de desglose detallado de XP por juego
        if (showXpBreakdownDialog) {
            XpBreakdownDialog(
                playerStats = playerStats,
                onDismiss = { showXpBreakdownDialog = false }
            )
        }

        // Diálogo explicativo de racha de días activos
        if (showStreakInfoDialog) {
            StreakInfoDialog(
                streakDays = playerStats.streakDays,
                onDismiss = { showStreakInfoDialog = false }
            )
        }
    }
}

/**
 * Encabezado con logo a la izquierda, saludo "Hola Miguel" y toggle GAME/TRAIN a la derecha.
 */
@Composable
private fun HomeHeaderRow(
    userGreeting: String = "Hola Miguel",
    avatarUrl: String? = null,
    isGameMode: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val greetingTextColor = if (isGameMode) Color(0xFF1E2229) else Color.White

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LADO IZQUIERDO: Logo Hoopstars / Avatar del usuario + "Hola Miguel"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onProfileClick() }
        ) {
            // Avatar / Logo en la izquierda: Contorno de colores en GAME y negro en PRO + imagen real de usuario
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
                        Color(0xFF000000),
                        Color(0xFF000000)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .testTag("home_avatar_badge")
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarBorderBrush)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                UserAvatarImage(
                    avatarUrl = avatarUrl,
                    displayName = userGreeting.removePrefix("Hola "),
                    fallbackDrawable = R.drawable.avatarchico,
                    size = 40.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = userGreeting,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.3).sp,
                color = greetingTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // LADO DERECHO: Toggle GAME / PRO
        GameTrainToggle(
            isGameMode = isGameMode,
            onToggle = onToggleMode
        )
    }
}

/**
 * Pulsador / Switch interactivo táctil para alternar entre modo GAME y modo PRO.
 * Presenta un diseño de botón físico/switch con deslizador animado,
 * y un relieve que destaca claramente cuál está activo.
 */
@Composable
fun GameTrainToggle(
    isGameMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dimensiones del pulsador
    val trackWidth = 142.dp
    val trackHeight = 38.dp
    val thumbWidth = 69.dp
    val thumbHeight = 32.dp

    // Desplazamiento horizontal animado del thumb
    val thumbOffset by animateDpAsState(
        targetValue = if (isGameMode) 3.dp else (trackWidth - thumbWidth - 3.dp),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "toggle_thumb_offset"
    )

    // Si es GAME: #2FB2C9. Si es PRO: naranja de baloncesto (#EA580C)
    val selectedColor = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFFEA580C)
    // Fondo del toggle: #befdff en GAME, y negro cuando pulsas PRO
    val trackBackgroundColor = if (isGameMode) Color(0xFFBEFDFF) else Color(0xFF000000)

    Box(
        modifier = modifier
            .testTag("game_train_toggle")
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackBackgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggle(!isGameMode)
            }
    ) {
        // 1. Deslizador físico / Thumb activo sin bordes
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 3.dp)
                .width(thumbWidth)
                .height(thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(selectedColor)
        )

        // 2. Fila con las 2 opciones (GAME / PRO) sin iconos ni bordes
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Opción GAME: blanco cuando está activo o cuando PRO está seleccionado (fondo negro)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggle(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GAME",
                    fontSize = 11.sp,
                    fontWeight = if (isGameMode) FontWeight.Black else FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = if (isGameMode) Color.White else Color.White
                )
            }

            // Opción PRO: blanco cuando está seleccionado, o texto oscuro sobre fondo turquesa claro
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggle(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRO",
                    fontSize = 11.sp,
                    fontWeight = if (!isGameMode) FontWeight.Black else FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = if (!isGameMode) Color.White else Color(0xFF0F4C5C)
                )
            }
        }
    }
}

/**
 * Banner superior de incentivo:
 * - Modo GAME: Magenta vibrante (#d93b98) con texto blanco
 * - Modo TRAIN: Negro (#1A1A1A o #000000) con borde sutil y texto blanco
 */
@Composable
private fun HomeIncentiveBanner(
    text: String,
    isGameMode: Boolean,
    modifier: Modifier = Modifier
) {
    val bannerBg = if (isGameMode) Color(0xFFD93B98) else Color(0xFF1E1E1E)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (!isGameMode) Modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)) else Modifier)
            .background(bannerBg)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Banner de imagen gráfico que utiliza banner1.png ajustado a la pantalla con bordes redondeados.
 */
@Composable
private fun HomeImageBanner(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .testTag("home_image_banner"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.banner1),
            contentDescription = "Banner Publicitario",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

/**
 * Slide Horizontal que contiene las tarjetas Hero de entrenamiento.
 * Permite deslizar horizontalmente entre las tarjetas. Cada tarjeta muestra
 * completamente la imagen point.png sin ningún texto superpuesto ni elementos externos.
 */
@Composable
private fun HeroCardsSlider(
    slides: List<HeroWorkoutSlide>,
    isGameMode: Boolean = true,
    onTransitionToCard: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    var lastRecordedPage by remember { mutableStateOf(pagerState.currentPage) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { newPage ->
            if (newPage != lastRecordedPage) {
                onTransitionToCard(lastRecordedPage, newPage)
                lastRecordedPage = newPage
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val slide = slides[page]
            val isCurrentPage = pagerState.currentPage == page
            SingleHeroWorkoutCard(
                slide = slide,
                isGameMode = isGameMode,
                isActive = isCurrentPage,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Indicador de paginación sutil para el slider (naranja en modo PRO, azul #0D63F3 en modo GAME)
        val selectedIndicatorColor = if (isGameMode) Color(0xFF0D63F3) else Color(0xFFEA580C)
        val unselectedIndicatorColor = if (isGameMode) Color(0xFFD1D5DB) else Color(0xFF374151)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until slides.size) {
                val isSelected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) selectedIndicatorColor else unselectedIndicatorColor)
                )
            }
        }
    }
}

/**
 * Tarjeta Hero individual del slider.
 * Muestra la imagen point.png completa con bordes redondeados y un botón central
 * de tamaño normal con animación estilo bouncer (rebote suave continuo) para "¡JUGAR AHORA!".
 */
@Composable
private fun SingleHeroWorkoutCard(
    slide: HeroWorkoutSlide,
    isGameMode: Boolean = true,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Fase 1: Nombre del juego con bounce de movimiento (2 segundos)
    // Fase 2: Botón ¡JUGAR AHORA! permanente hasta cambiar de tarjeta
    var showTitlePhase by remember { mutableStateOf(true) }

    val titleScale = remember { Animatable(0.35f) }
    val titleOffsetY = remember { Animatable(32f) }
    val titleAlpha = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            showTitlePhase = true
            titleScale.snapTo(0.35f)
            titleOffsetY.snapTo(32f)
            titleAlpha.snapTo(0f)

            val springSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            launch {
                titleAlpha.animateTo(1f, tween(160))
            }
            launch {
                titleOffsetY.animateTo(0f, springSpec)
            }
            launch {
                titleScale.animateTo(1f, springSpec)
            }

            // Permanece visible mostrando el nombre del juego durante 2 segundos
            delay(2000L)

            // Desaparición suave
            launch {
                titleAlpha.animateTo(0f, tween(200))
            }
            launch {
                titleScale.animateTo(0.85f, tween(200))
            }
            delay(200L)
            showTitlePhase = false
        } else {
            // Al cambiar de tarjeta, se resetea para que la próxima vez vuelva a salir primero el nombre
            showTitlePhase = true
            titleScale.snapTo(0.35f)
            titleOffsetY.snapTo(32f)
            titleAlpha.snapTo(0f)
        }
    }

    // Animación continua estilo bouncer (rebote elástico suave y pulsación de escala) para el botón de jugar
    val infiniteTransition = rememberInfiniteTransition(label = "hero_bouncer_transition")
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_bouncer_scale"
    )
    val bounceOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_bouncer_offset_y"
    )

    // Animación de entrada suave para el botón de jugar una vez que desaparece el título
    val buttonAlpha by animateFloatAsState(
        targetValue = if (!showTitlePhase) 1f else 0f,
        animationSpec = tween(280),
        label = "hero_play_btn_alpha"
    )
    val buttonEntranceScale by animateFloatAsState(
        targetValue = if (!showTitlePhase) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "hero_play_btn_scale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                if (!slide.isLocked) {
                    slide.onAction()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Imagen base de la tarjeta (en modo bloqueado se muestra en escala de grises atenuada)
        val grayscaleFilter = remember {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.05f) })
        }
        Image(
            painter = painterResource(id = slide.imageResId),
            contentDescription = slide.drillName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop,
            colorFilter = if (slide.isLocked) grayscaleFilter else null
        )

        if (slide.isLocked) {
            // Fondo gris oscuro semitransparente que bloquea visualmente la tarjeta
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD91F2937)) // Gris oscuro carbón elegante
            )

            // Cartel central translúcido con candado y XP requerida
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xCC0F172A)) // Cartel oscuro tipo vidrio esmerilado
                    .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Candado dorado/bronce centrado con halo circular sutil
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0x40EAB308),
                                        Color(0x10EAB308),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFD97706), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Texto instructivo "Sube de nivel para desbloquear"
                    Text(
                        text = "Sube de nivel para desbloquear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barra de progreso de XP (turquesa #2FB2C9 con base oscura)
                    val progress = remember(slide.currentXp, slide.requiredXp) {
                        if (slide.requiredXp > 0) {
                            (slide.currentXp.toFloat() / slide.requiredXp.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF2FB2C9),
                                            Color(0xFF56D6EB)
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cartel REQUERIDO: XXX Hype/XP en turquesa (#2FB2C9)
                    Text(
                        text = if (isGameMode) "REQUERIDO: ${slide.requiredXp} Hype" else "REQUERIDO: ${slide.requiredXp} XP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color(0xFF2FB2C9),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Indicador de Hype/XP actual únicamente (texto del nivel quitado)
                    Text(
                        text = if (isGameMode) "Hype actual: ${slide.currentXp}" else "XP actual: ${slide.currentXp}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Fase 1: Nombre del juego en letras grandes con profundidad de color blanco y tamaño medio
            if (showTitlePhase || titleAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .offset(y = titleOffsetY.value.dp)
                        .scale(titleScale.value)
                        .alpha(titleAlpha.value)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Color(0x73000000),
                            spotColor = Color(0xB3000000)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xEE0F172A),
                                    Color(0xF8020617)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0x99FFFFFF),
                                    Color(0x25FFFFFF)
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { slide.onAction() }
                        .padding(horizontal = 26.dp, vertical = 14.dp)
                        .testTag("hero_card_game_title"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Sombra inferior de relieve para profundidad 3D
                        Text(
                            text = slide.drillName.uppercase(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 1.5.sp,
                            color = Color.Black.copy(alpha = 0.85f),
                            modifier = Modifier.offset(y = 2.dp)
                        )
                        // Texto principal en color blanco puro con relieve y sombra de profundidad
                        Text(
                            text = slide.drillName.uppercase(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 1.5.sp,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.95f),
                                    offset = Offset(0f, 2.5f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
            }

            // Fase 2: Botón central ¡JUGAR AHORA! permanente tras los 2 segundos con bouncer continuo
            if (!showTitlePhase || buttonAlpha > 0.01f) {
                val buttonGradient = if (isGameMode) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF2FB2C9),
                            Color(0xFF0F869B)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFEA580C),
                            Color(0xFFC2410C)
                        )
                    )
                }

                val shadowAmbient = if (isGameMode) Color(0x662FB2C9) else Color(0x66EA580C)
                val shadowSpot = if (isGameMode) Color(0x990F869B) else Color(0x99C2410C)

                Box(
                    modifier = Modifier
                        .offset(y = bounceOffsetY.dp)
                        .scale(bounceScale * buttonEntranceScale)
                        .alpha(buttonAlpha)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = shadowAmbient,
                            spotColor = shadowSpot
                        )
                        .clip(RoundedCornerShape(50))
                        .background(buttonGradient)
                        .clickable { slide.onAction() }
                        .padding(horizontal = 28.dp, vertical = 15.dp)
                        .testTag("hero_card_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isGameMode) "¡JUGAR AHORA!" else "¡ENTRENAR AHORA!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila con las 3 tarjetas de gamificación (Rookie/Rank, Días de Racha, XP)
 * idéntica a la imagen de referencia con badges circulares flotantes superiores.
 */
@Composable
private fun GamificationCardsRow(
    flameGlowAlpha: Float,
    rankTitle: String = "ROOKIE",
    level: Int = 1,
    streakDays: Int = 0,
    totalXp: Int = 0,
    onRankClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    onXpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. ROOKIE / RANK CARD (#e15f25 con escudo oficial correspondiente)
        val currentShieldRes = when {
            totalXp >= 3000 || rankTitle.contains("GOAT", ignoreCase = true) -> R.drawable.goat
            totalXp >= 1500 || rankTitle.contains("MVP", ignoreCase = true) -> R.drawable.mvp
            totalXp >= 750 || rankTitle.contains("ALL-STAR", ignoreCase = true) -> R.drawable.allstar
            else -> R.drawable.rookie
        }
        val rookieColor = Color(0xFFE15F25)

        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = onRankClick != null) { onRankClick?.invoke() },
            contentAlignment = Alignment.TopCenter
        ) {
            // Cuerpo de la tarjeta redondeada en color #e15f25 con la misma altura exacta que las demás
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(rookieColor)
                    .padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Nivel $level",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // Escudo oficial que sobresale en la parte superior exactamente como en el diseño
            Image(
                painter = painterResource(id = currentShieldRes),
                contentDescription = rankTitle,
                modifier = Modifier.size(width = 50.dp, height = 56.dp),
                contentScale = ContentScale.Fit
            )
        }

        // 2. STREAK CARD (Cyan / Turquesa con imagen fuego.png fuera del círculo)
        val streakText = "$streakDays ${if (streakDays == 1) "DÍA" else "DÍAS"}"
        val streakSub = when {
            streakDays == 0 -> "Empieza hoy"
            streakDays == 1 -> "Primer día"
            else -> "No rompas racha"
        }
        val streakColor = Color(0xFF00ACC1)
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = onStreakClick != null) { onStreakClick?.invoke() },
            contentAlignment = Alignment.TopCenter
        ) {
            // Cuerpo de la tarjeta redondeada en color turquesa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(streakColor)
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    Text(
                        text = streakText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = streakSub,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xEEFFFFFF),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Imagen del fuego oficial ajustada para dejar espacio al texto grande
            Image(
                painter = painterResource(id = R.drawable.fuego),
                contentDescription = "Racha",
                modifier = Modifier.size(width = 42.dp, height = 46.dp),
                contentScale = ContentScale.Fit
            )
        }

        // 3. HYPE CARD (Royal Blue con cohete fuera del círculo)
        val hypeColor = Color(0xFF1E88E5)
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = onXpClick != null) { onXpClick?.invoke() },
            contentAlignment = Alignment.TopCenter
        ) {
            // Cuerpo de la tarjeta redondeada en color azul
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(hypeColor)
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    Text(
                        text = "$totalXp",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Hype",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xEEFFFFFF),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Imagen del cohete oficial ajustada para dejar espacio al texto grande
            Image(
                painter = painterResource(id = R.drawable.cohete),
                contentDescription = "Hype",
                modifier = Modifier.size(width = 42.dp, height = 46.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Componente individual de tarjeta con badge flotante superior.
 */
@Composable
private fun GamificationBadgeCard(
    topBadgeContent: @Composable () -> Unit,
    title: String,
    subtitle: String,
    cardColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier

    Box(
        modifier = clickableModifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // Cuerpo de la tarjeta con esquinas redondeadas y altura sincronizada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .padding(top = 26.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xEEFFFFFF),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Badge flotante en la parte superior que sobresale hacia arriba
        Box(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            topBadgeContent()
        }
    }
}

/**
 * Sección de telemetría y métricas reales guardadas para el MODO PRO en la portada debajo del slide.
 * Muestra contadores oficiales de tiros lanzados, canastas anotadas, porcentaje de acierto,
 * entrenamientos completados, tiempo acumulado de práctica y racha diaria.
 */
@Composable
private fun ProDashboardStatsSection(
    playerStats: com.example.stats.PlayerStats,
    onNavigateToWorkouts: () -> Unit,
    onOpenXpBreakdown: () -> Unit,
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ENCABEZADO PRO
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MÉTRICAS OFICIALES PRO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF97316).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFF97316).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REAL DATA",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF97316)
                        )
                    }
                }
                Text(
                    text = "Datos reales acumulados por visión artificial",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Chip indicador verde
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "GUARDADOS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        // GRID DE CONTADORES PRO (3 filas de 2 columnas)
        // 1. Tiros Lanzados | Canastas Anotadas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProMetricStatCard(
                value = "${playerStats.shootingShots}",
                label = "Tiros Lanzados",
                subtext = "Total lanzamientos",
                icon = Icons.Default.SportsBasketball,
                accentColor = Color(0xFFF97316),
                modifier = Modifier.weight(1f)
            )
            ProMetricStatCard(
                value = "${playerStats.shootingMakes}",
                label = "Canastas Anotadas",
                subtext = "Aciertos confirmados",
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        // 2. % Precisión | Entrenamientos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProMetricStatCard(
                value = "${playerStats.shootingAccuracyPct}%",
                label = "Efectividad Tiro",
                subtext = if (playerStats.shootingShots > 0) "${playerStats.shootingMakes} de ${playerStats.shootingShots} tiros" else "Sin tiros aún",
                icon = Icons.Default.Speed,
                accentColor = Color(0xFF06B6D4),
                modifier = Modifier.weight(1f)
            )
            ProMetricStatCard(
                value = "${playerStats.totalSessionsCount}",
                label = "Entrenamientos",
                subtext = "${playerStats.shootingSessions} tiros • ${playerStats.totalMinigamesCount} drills",
                icon = Icons.Default.FitnessCenter,
                accentColor = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Tiempo en Pista | Racha Diaria
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val formattedTime = if (playerStats.totalTrainingMinutes >= 60) {
                "${playerStats.totalTrainingMinutes / 60}h ${playerStats.totalTrainingMinutes % 60}m"
            } else {
                "${playerStats.totalTrainingMinutes} min"
            }
            ProMetricStatCard(
                value = formattedTime,
                label = "Tiempo en Pista",
                subtext = "Práctica activa real",
                icon = Icons.Default.Timer,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            ProMetricStatCard(
                value = "${playerStats.streakDays} ${if (playerStats.streakDays == 1) "Día" else "Días"}",
                label = "Racha Diaria",
                subtext = if (playerStats.streakDays > 0) "Constancia activa 🔥" else "Empieza hoy",
                icon = Icons.Default.Whatshot,
                accentColor = Color(0xFFEF4444),
                onClick = onStreakClick,
                modifier = Modifier.weight(1f)
            )
        }

        // CARD DE CONTROL DE XP Y NIVEL DEL ATLETA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF16161B))
                .border(1.dp, Color(0xFF282832), RoundedCornerShape(18.dp))
                .clickable { onOpenXpBreakdown() }
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF60A5FA)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NIVEL ${playerStats.level} • ${playerStats.rankTitle}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${playerStats.totalXp} XP totales registrados",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262633))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DESGLOSE XP →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playerStats.rankSubtitle,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = if (playerStats.xpToNextRank > 0) "Faltan ${playerStats.xpToNextRank} XP" else "¡Nivel Máximo!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { playerStats.rankProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF262633)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fila con resumen de los 5 juegos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProGameMiniChip(label = "Tiro", xp = "${playerStats.xpShooting}", color = Color(0xFFF97316))
                    ProGameMiniChip(label = "Reaction", xp = "${playerStats.xpReactionPoints}", color = Color(0xFF06B6D4))
                    ProGameMiniChip(label = "Dribble", xp = "${playerStats.xpDribbleCombo}", color = Color(0xFF8B5CF6))
                    ProGameMiniChip(label = "Defend", xp = "${playerStats.xpDefendZone}", color = Color(0xFF10B981))
                    ProGameMiniChip(label = "Kids", xp = "${playerStats.xpKidsMiniBasket}", color = Color(0xFFD93B98))
                }
            }
        }

        // BOTÓN ACCIÓN RÁPIDA: ENTRENAR
        Button(
            onClick = onNavigateToWorkouts,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF97316),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "INICIAR ENTRENAMIENTO O DRILL PRO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ProMetricStatCard(
    value: String,
    label: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickableMod = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier

    Box(
        modifier = clickableMod
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16161B))
            .border(1.dp, Color(0xFF282832), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProGameMiniChip(
    label: String,
    xp: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = xp,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8)
        )
    }
}

/**
 * Diálogo que desglosa detalladamente de dónde se generan los puntos de XP en cada uno de los juegos.
 */
@Composable
private fun XpBreakdownDialog(
    playerStats: com.example.stats.PlayerStats,
    onDismiss: () -> Unit
) {
    val breakdown = remember(playerStats) { playerStats.getXpBreakdown() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF282832), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONTROL TOTAL DE HYPE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Registro oficial por juego y disciplina",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta resumen de nivel y rango
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RANGO ${playerStats.rankTitle}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF93C5FD),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${playerStats.totalXp} Hype",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "NIVEL ${playerStats.level}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = playerStats.rankSubtitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { playerStats.rankProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Base: ${playerStats.currentRankBaseXp} Hype",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = if (playerStats.xpToNextRank > 0) "Siguiente: ${playerStats.nextRankXp} Hype (${playerStats.xpToNextRank} faltan)" else "¡Máximo Rango!",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Desglose por juego
                Text(
                    text = "ORIGEN DE PUNTOS HYPE POR JUEGO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                breakdown.forEach { source ->
                    val sourceColor = Color(source.colorHex)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A1A20))
                            .border(1.dp, Color(0xFF282832), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = source.emoji,
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = source.gameName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = source.category,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+${source.xpPoints} Hype",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = sourceColor
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f%% del total", source.percentageOfTotal),
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = source.detailText,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF282832),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Diálogo explicativo del sistema de racha de días activos.
 */
@Composable
private fun StreakInfoDialog(
    streakDays: Int,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF282832), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔥",
                    fontSize = 44.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$streakDays ${if (streakDays == 1) "DÍA DE RACHA" else "DÍAS DE RACHA"}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = if (streakDays > 0) "¡Tu fuego sigue encendido!" else "Inicia hoy tu racha diaria",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A20))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "📌 Reglas de Racha Kantera:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316)
                        )
                        Text(
                            text = "• Cada día consecutivo que entras a la app sumas +1 día a tu racha.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "• Al crear una nueva cuenta, la racha empieza estrictamente desde 0.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "• Si dejas de entrar un día entero, la racha se reinicia y vuelve a empezar desde 1 al volver a entrar.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF97316),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¡Entendido!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Barra de navegación inferior:
 * - Modo GAME: Barra en color turquesa (#2FB2C9).
 * - Modo PRO/TRAIN: Barra en color negro (#111114).
 * - Todos los elementos (Inicio, The Court, Workout, Perfil) integrados armónicamente en el diseño del menú.
 * - Modo GAME: Barra en color turquesa (#2FB2C9).
 * - Modo PRO/TRAIN: Barra en color negro (#111114).
 * - Resaltado sutil pero claro del icono de la pantalla activa mediante una cápsula translúcida,
 *   escala suave, contraste de opacidad y punto indicador activo.
 */
@Composable
private fun HomeBottomNavigationBar(
    selectedTab: HomeBottomTab,
    isGameMode: Boolean,
    onTabSelected: (HomeBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barBackgroundColor = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFF111114)
    val topBorderColor = if (isGameMode) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Barra de navegación con línea superior sutil, sin contornos laterales ni inferiores
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(barBackgroundColor)
                .drawBehind {
                    drawLine(
                        color = topBorderColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Inicio
                HomeNavItem(
                    tab = HomeBottomTab.HOME,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = "Inicio",
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        BasketballBallNavIcon(
                            color = tint,
                            modifier = iconMod
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // 2. Stats (en Modo PRO) / Battle (en Modo GAME)
                val secondTabLabel = if (isGameMode) "Battle" else "Stats"
                HomeNavItem(
                    tab = HomeBottomTab.THE_COURT,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = secondTabLabel,
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        if (isGameMode) {
                            BattleNavIcon(
                                color = tint,
                                modifier = iconMod
                            )
                        } else {
                            StatsNavIcon(
                                color = tint,
                                modifier = iconMod
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 3. Workout
                HomeNavItem(
                    tab = HomeBottomTab.WORKOUT,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = "Workout",
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        BasketballHoopNavIcon(
                            color = tint,
                            modifier = iconMod
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // 4. Perfil
                HomeNavItem(
                    tab = HomeBottomTab.PROFILE,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = "Perfil",
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        Icon(
                            imageVector = if (selectedTab == HomeBottomTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Perfil",
                            tint = tint,
                            modifier = iconMod
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Elemento individual del menú inferior con anchura fija equitativa (25%),
 * sin animaciones de escala ni re-flow de tipografía para evitar cualquier temblor o desplazamiento,
 * y sin efecto de brillo/degradado en ninguna versión (GAME ni PRO).
 */
@Composable
private fun HomeNavItem(
    tab: HomeBottomTab,
    selectedTab: HomeBottomTab,
    isGameMode: Boolean,
    label: String,
    onTabSelected: (HomeBottomTab) -> Unit,
    icon: @Composable (tint: Color, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedTab == tab

    // Tono del icono
    val iconColor = if (isSelected) {
        Color.White
    } else {
        if (isGameMode) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f)
    }

    // Tono del texto
    val textColor = if (isSelected) {
        Color.White
    } else {
        if (isGameMode) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.48f)
    }

    // Elemento completamente estable sin cambios de anchura ni de escala, ni brillo
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTabSelected(tab) }
            .testTag("nav_tab_${label.lowercase().replace(" ", "_")}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon(
                iconColor,
                Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatsNavIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.20f
        val radius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())

        // Barra 1 (baja)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.52f),
            size = Size(barWidth, h * 0.38f),
            cornerRadius = radius
        )
        // Barra 2 (media)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.40f, h * 0.28f),
            size = Size(barWidth, h * 0.62f),
            cornerRadius = radius
        )
        // Barra 3 (alta)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.70f, h * 0.12f),
            size = Size(barWidth, h * 0.78f),
            cornerRadius = radius
        )
    }
}

@Composable
private fun BattleNavIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val guardStroke = 1.8.dp.toPx()

        // Espada 1 (diagonal de arriba-izq a abajo-der)
        drawLine(
            color = color,
            start = Offset(w * 0.20f, h * 0.20f),
            end = Offset(w * 0.80f, h * 0.80f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Guarda espada 1
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.76f),
            end = Offset(w * 0.76f, h * 0.58f),
            strokeWidth = guardStroke,
            cap = StrokeCap.Round
        )

        // Espada 2 (diagonal de arriba-der a abajo-izq)
        drawLine(
            color = color,
            start = Offset(w * 0.80f, h * 0.20f),
            end = Offset(w * 0.20f, h * 0.80f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Guarda espada 2
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.76f),
            end = Offset(w * 0.24f, h * 0.58f),
            strokeWidth = guardStroke,
            cap = StrokeCap.Round
        )

        // Pequeño brillo/chispa central de choque de combate
        drawCircle(
            color = color,
            radius = 1.8.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}

@Composable
private fun BasketballHoopNavIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val boardLeft = w * 0.15f
        val boardRight = w * 0.85f
        val boardTop = h * 0.15f
        val boardBottom = h * 0.48f

        drawRoundRect(
            color = color,
            topLeft = Offset(boardLeft, boardTop),
            size = Size(boardRight - boardLeft, boardBottom - boardTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        val innerLeft = w * 0.35f
        val innerRight = w * 0.65f
        val innerTop = h * 0.28f
        val innerBottom = h * 0.48f
        drawRect(
            color = color,
            topLeft = Offset(innerLeft, innerTop),
            size = Size(innerRight - innerLeft, innerBottom - innerTop),
            style = Stroke(width = 1.2.dp.toPx())
        )

        val rimY = h * 0.50f
        val rimLeft = w * 0.30f
        val rimRight = w * 0.70f
        drawLine(
            color = color,
            start = Offset(rimLeft, rimY),
            end = Offset(rimRight, rimY),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )

        val netBottomY = h * 0.82f
        val netBottomLeft = w * 0.38f
        val netBottomRight = w * 0.62f

        drawLine(
            color = color,
            start = Offset(rimLeft, rimY),
            end = Offset(netBottomLeft, netBottomY),
            strokeWidth = 1.4.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(rimRight, rimY),
            end = Offset(netBottomRight, netBottomY),
            strokeWidth = 1.4.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(netBottomLeft, netBottomY),
            end = Offset(netBottomRight, netBottomY),
            strokeWidth = 1.4.dp.toPx()
        )
    }
}

@Composable
private fun BasketballBallNavIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = w * 0.44f

            // Círculo exterior
            drawCircle(
                color = color,
                radius = r,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Línea central horizontal
            drawLine(
                color = color,
                start = Offset(w * 0.08f, h * 0.5f),
                end = Offset(w * 0.92f, h * 0.5f),
                strokeWidth = 1.2.dp.toPx()
            )

            // Línea central vertical
            drawLine(
                color = color,
                start = Offset(w * 0.5f, h * 0.08f),
                end = Offset(w * 0.5f, h * 0.92f),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        Text(
            text = "H★",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

/**
 * Cuadro de diálogo / Popup emergente idéntico al diseño de referencia:
 * - Fondo negro carbón (#151515) con esquinas redondeadas generosas (~28dp)
 * - Botón circular de cierre "X" en la esquina superior derecha
 * - Logo circular centrado de Hoopstars (balón multicolor turquesa/magenta/amarillo con H★)
 * - Título en negrita mayúsculas: "ENJOYING THE APP?"
 * - Subtítulo: "Switch to Pro and unlock all drills, levels, and premium features."
 * - Botón principal con color de fondo #2FB2C9: "BECOME A PRO"
 * - Enlace discreto abajo: "Maybe later"
 */
@Composable
private fun XpAdRewardPopup(
    onDismiss: () -> Unit,
    onWatchAdClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF141416))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Botón cerrar (X) en la esquina superior derecha con fondo circular oscuro
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF26262B))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color(0xFFD0D0D5),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // 1. Logo circular grande exactamente como en la imagen
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF2FB2C9),
                                    Color(0xFFD93B98),
                                    Color(0xFFFFD600),
                                    Color(0xFF2FB2C9)
                                )
                            )
                        )
                        .padding(3.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2FB2C9)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Curvas de balón / costuras con estilo
                        drawCircle(
                            color = Color(0xFFD93B98),
                            radius = w * 0.45f,
                            style = Stroke(width = 3.5f)
                        )
                        drawLine(
                            color = Color(0xFF111827),
                            start = Offset(0f, h * 0.5f),
                            end = Offset(w, h * 0.5f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFF111827),
                            start = Offset(w * 0.5f, 0f),
                            end = Offset(w * 0.5f, h),
                            strokeWidth = 3f
                        )
                    }

                    Text(
                        text = "H★",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF111827)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 2. Título principal idéntico a la imagen
                Text(
                    text = "ENJOYING THE APP?",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Subtítulo limpio y centrado
                Text(
                    text = "Switch to Pro and unlock all drills,\nlevels, and premium features.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFB5B5BE),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4. Botón principal con color de fondo #2FB2C9
                Button(
                    onClick = onWatchAdClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2FB2C9)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "BECOME A PRO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. Enlace "Maybe later"
                Text(
                    text = "Maybe later",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E98),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Diálogo popup informativo para el MODO PRO:
 * - Fondo negro profundo (#141416) idéntico al estilo del popup de tarjetas.
 * - Icono superior con aro estilizado en naranja PRO (#EA580C) e insignia "PRO".
 * - Explica que el Modo PRO está diseñado para entrenar sin distracciones:
 *   sin clasificaciones, sin XP ni ruidos, enfocado 100% en la práctica con análisis e informes avanzados solo para ti.
 */
@Composable
private fun ProModeInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF141416))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Botón cerrar (X) en la esquina superior derecha
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF26262B))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = Color(0xFFD0D0D5),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // 1. Icono circular con halo naranja PRO
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFEA580C),
                                    Color(0xFFFF7A00),
                                    Color(0xFFF59E0B),
                                    Color(0xFFEA580C)
                                )
                            )
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1917)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Líneas baloncestísticas sutiles
                        drawCircle(
                            color = Color(0xFFEA580C).copy(alpha = 0.4f),
                            radius = w * 0.44f,
                            style = Stroke(width = 2.5f)
                        )
                        drawLine(
                            color = Color(0xFFEA580C).copy(alpha = 0.5f),
                            start = Offset(0f, h * 0.5f),
                            end = Offset(w, h * 0.5f),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFFEA580C).copy(alpha = 0.5f),
                            start = Offset(w * 0.5f, 0f),
                            end = Offset(w * 0.5f, h),
                            strokeWidth = 2f
                        )
                    }

                    Text(
                        text = "PRO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color(0xFFEA580C)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Título principal en blanco
                Text(
                    text = "BIENVENIDO AL MODO PRO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Subtítulo destacando el entrenamiento sin distracciones
                Text(
                    text = "Tu espacio de alto rendimiento enfocado 100% en ti.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEA580C),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Tarjetas de características clave
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProFeatureRow(
                        bullet = "⚡",
                        title = "Cero distracciones",
                        description = "Sin tablas de clasificación ni puntos XP. Toda la atención puesta en la pista y en cada repetición."
                    )
                    ProFeatureRow(
                        bullet = "📊",
                        title = "Análisis e informes avanzados",
                        description = "Estadísticas detalladas de tu precisión, velocidad y postura registradas de forma privada y exclusiva para ti."
                    )
                    ProFeatureRow(
                        bullet = "🎯",
                        title = "Entrenamiento directo",
                        description = "Acceso inmediato a todos los drills y modos tácticos diseñados para llevar tu juego al siguiente nivel."
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Botón de acción principal en naranja PRO
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "ENTENDIDO, A ENTRENAR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProFeatureRow(
    bullet: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1D1D22))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = bullet,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF9CA3AF),
                lineHeight = 16.sp
            )
        }
    }
}


