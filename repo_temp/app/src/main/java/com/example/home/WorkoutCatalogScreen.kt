package com.example.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.stats.PlayerStats
import com.example.ui.common.UserAvatarImage

/**
 * Categorías de filtro para el catálogo de entrenamientos
 */
private enum class WorkoutFilterCategory(val label: String) {
    ALL("Todos"),
    UNLOCKED("Disponibles"),
    SHOOTING("Tiro"),
    DRIBBLE("Bote"),
    DEFENSE("Defensa"),
    KIDS("Kids")
}

/**
 * Pantalla completa de catálogo de Workouts y Juegos:
 * - Mismo diseño y estética de la pantalla principal (fondo, tipografía deportiva, esquinas, colores)
 * - Muestra los juegos del slide en tamaño más pequeño (cuadrícula de 2 columnas o lista) para visualizarlos fácilmente
 * - Acceso directo a cada juego
 */
@Composable
fun WorkoutCatalogScreen(
    slides: List<HeroWorkoutSlide>,
    isGameMode: Boolean,
    playerName: String,
    avatarUrl: String?,
    playerStats: PlayerStats,
    onBackToHome: () -> Unit = {},
    onProfileClick: () -> Unit,
    onOpenXpBreakdown: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(WorkoutFilterCategory.ALL) }
    var isGridView by remember { mutableStateOf(true) }
    var lockedSlideDetails by remember { mutableStateOf<HeroWorkoutSlide?>(null) }

    // Paleta sincronizada exactamente con la Home
    val screenBg = if (isGameMode) Color.White else Color(0xFF121212)
    val textPrimary = if (isGameMode) Color(0xFF1E2229) else Color.White
    val textSecondary = if (isGameMode) Color(0xFF64748B) else Color(0xFF94A3B8)
    val primaryAccent = if (isGameMode) Color(0xFFD93B98) else Color(0xFFEA580C)
    val secondaryAccent = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFFFFB020)

    // Filtrar los juegos según la categoría seleccionada
    val filteredSlides = remember(slides, selectedCategory) {
        when (selectedCategory) {
            WorkoutFilterCategory.ALL -> slides
            WorkoutFilterCategory.UNLOCKED -> slides.filter { !it.isLocked }
            WorkoutFilterCategory.SHOOTING -> slides.filter {
                it.drillName.contains("SHOOT", ignoreCase = true) ||
                it.drillName.contains("BASKET", ignoreCase = true) ||
                it.title.contains("TIRO", ignoreCase = true)
            }
            WorkoutFilterCategory.DRIBBLE -> slides.filter {
                it.drillName.contains("DRIBBL", ignoreCase = true) ||
                it.drillName.contains("CROSSOVER", ignoreCase = true) ||
                it.drillName.contains("REACTION", ignoreCase = true) ||
                it.drillName.contains("TOUCH", ignoreCase = true)
            }
            WorkoutFilterCategory.DEFENSE -> slides.filter {
                it.drillName.contains("DEFEND", ignoreCase = true) ||
                it.drillName.contains("AGILITY", ignoreCase = true) ||
                it.drillName.contains("LASER", ignoreCase = true)
            }
            WorkoutFilterCategory.KIDS -> slides.filter {
                it.drillName.contains("MINI", ignoreCase = true) ||
                it.difficulty.contains("KIDS", ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. CABECERA SUPERIOR FIJA: Exactamente la misma posición, padding y altura que en Inicio y Perfil
        WorkoutHeaderRow(
            playerName = playerName,
            avatarUrl = avatarUrl,
            isGameMode = isGameMode,
            playerStats = playerStats,
            onToggleMode = onToggleMode,
            onProfileClick = onProfileClick,
            onXpClick = onOpenXpBreakdown,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(44.dp)
        )

        LazyVerticalGrid(
            columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("workout_catalog_grid")
        ) {
            // Barra con filtros y toggle de vista (Cuadrícula / Lista)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Barra con filtros y toggle de vista (Cuadrícula / Lista)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Chips de categorías desplazables horizontalmente
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(WorkoutFilterCategory.values()) { category ->
                                val isSelected = category == selectedCategory
                                val chipBg = if (isSelected) {
                                    primaryAccent
                                } else {
                                    if (isGameMode) Color(0xFFF1F5F9) else Color(0xFF1E2433)
                                }
                                val chipTextColor = if (isSelected) {
                                    Color.White
                                } else {
                                    if (isGameMode) Color(0xFF475569) else Color(0xFF94A3B8)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(chipBg)
                                        .clickable { selectedCategory = category }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = category.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = chipTextColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón para alternar entre cuadrícula y lista
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isGameMode) Color(0xFFF1F5F9) else Color(0xFF1E2433))
                                .testTag("btn_toggle_view_mode")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Cambiar vista",
                                tint = if (isGameMode) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 2. ITEMS DEL CATÁLOGO DE ENTRENAMIENTO (En tamaño más pequeño)
            items(filteredSlides, key = { it.drillName + it.levelNumber }) { slide ->
                if (isGridView) {
                    CompactWorkoutGridCard(
                        slide = slide,
                        isGameMode = isGameMode,
                        primaryAccent = primaryAccent,
                        secondaryAccent = secondaryAccent,
                        onLockedClick = { lockedSlideDetails = slide }
                    )
                } else {
                    CompactWorkoutListCard(
                        slide = slide,
                        isGameMode = isGameMode,
                        primaryAccent = primaryAccent,
                        secondaryAccent = secondaryAccent,
                        onLockedClick = { lockedSlideDetails = slide }
                    )
                }
            }
        }

        // Diálogo para entrenamientos bloqueados
        lockedSlideDetails?.let { slide ->
            LockedWorkoutDialog(
                slide = slide,
                playerTotalXp = playerStats.totalXp,
                isGameMode = isGameMode,
                primaryAccent = primaryAccent,
                onDismiss = { lockedSlideDetails = null }
            )
        }
    }
}

/**
 * Cabecera con Avatar de usuario, Píldoras de racha y XP a la izquierda, y Toggle GAME/PRO a la derecha
 */
@Composable
fun WorkoutHeaderRow(
    playerName: String,
    avatarUrl: String?,
    isGameMode: Boolean,
    playerStats: PlayerStats,
    onToggleMode: (Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onXpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LADO IZQUIERDO: Avatar + Badges de Racha y XP desplazados a la izquierda
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Avatar con contorno estilizado según el modo activo
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarBorderBrush)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                UserAvatarImage(
                    avatarUrl = avatarUrl,
                    displayName = playerName,
                    fallbackDrawable = R.drawable.avatarchico,
                    size = 40.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Badges de Racha
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isGameMode) Color(0xFFFFF7ED) else Color(0xFF1E2433))
                    .border(1.dp, Color(0xFFFF6B1A).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Racha",
                        tint = Color(0xFFFF6B1A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${playerStats.streakDays}d",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isGameMode) Color(0xFFC2410C) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Badges de Hype (en Modo GAME con icono cohete) o XP (en Modo TRAIN/PRO)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isGameMode) Color(0xFFE0F7FA) else Color(0xFF1E2433))
                    .border(1.dp, Color(0xFF00BCD4).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onXpClick)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isGameMode) {
                        Image(
                            painter = painterResource(id = R.drawable.cohete),
                            contentDescription = "Hype",
                            modifier = Modifier.size(15.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${playerStats.totalXp} Hype",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00838F)
                        )
                    } else {
                        Text(
                            text = "${playerStats.totalXp} XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // LADO DERECHO: Toggle GAME / PRO en la misma posición que en Home
        GameTrainToggle(
            isGameMode = isGameMode,
            onToggle = onToggleMode
        )
    }
}

/**
 * Tarjeta compacta en formato CUADRÍCULA (2 columnas):
 * Presenta el juego de forma visualmente atractiva y fácilmente escaneable en tamaño reducido
 */
@Composable
private fun CompactWorkoutGridCard(
    slide: HeroWorkoutSlide,
    isGameMode: Boolean,
    primaryAccent: Color,
    secondaryAccent: Color,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = slide.isLocked
    val cardBg = if (isGameMode) {
        if (isLocked) Color(0xFFF1F5F9) else Color(0xFFFFFFFF)
    } else {
        if (isLocked) Color(0xFF0F131C) else Color(0xFF141926)
    }
    val cardBorder = if (isGameMode) {
        if (isLocked) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)
    } else {
        if (isLocked) Color(0xFF1E2536) else Color(0xFF28324A)
    }
    val titleColor = if (isGameMode) Color(0xFF0F172A) else Color.White
    val subtitleColor = if (isGameMode) Color(0xFF334155) else Color(0xFFE2E8F0)
    val mutedColor = if (isGameMode) Color(0xFF64748B) else Color(0xFF94A3B8)

    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.05f) })
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isGameMode && !isLocked) 2.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isLocked) {
                    onLockedClick()
                } else {
                    slide.onAction()
                }
            }
            .testTag("workout_card_${slide.drillName.lowercase().replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Contenedor de la imagen con badges superpuestos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.22f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                Image(
                    painter = painterResource(id = slide.imageResId),
                    contentDescription = slide.drillName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (isLocked) grayscaleFilter else null
                )

                // Sombra degradada para lectura
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.70f)
                                )
                            )
                        )
                )

                // Badge de dificultad arriba a la izquierda
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(
                            0.8.dp,
                            if (isLocked) Color(0xFF64748B) else secondaryAccent.copy(alpha = 0.8f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = slide.difficulty,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLocked) Color(0xFF94A3B8) else secondaryAccent
                    )
                }

                // Badge de duración arriba a la derecha
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = slide.duration,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Si está bloqueado, superpone candado con nivel
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NIVEL ${slide.levelNumber}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Información del juego en la parte inferior de la tarjeta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = slide.drillName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = slide.subtitle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    color = subtitleColor
                )

                if (isLocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGameMode) Color(0xFFE2E8F0) else Color(0xFF1E2536))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = mutedColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${slide.requiredXp} XP para desbloquear",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = mutedColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta compacta en formato LISTA (filas horizontales elegantes):
 * Permite visualizar el catálogo en formato de lista ultrarrápida de escanear
 */
@Composable
private fun CompactWorkoutListCard(
    slide: HeroWorkoutSlide,
    isGameMode: Boolean,
    primaryAccent: Color,
    secondaryAccent: Color,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = slide.isLocked
    val cardBg = if (isGameMode) {
        if (isLocked) Color(0xFFF1F5F9) else Color(0xFFFFFFFF)
    } else {
        if (isLocked) Color(0xFF0F131C) else Color(0xFF141926)
    }
    val cardBorder = if (isGameMode) {
        if (isLocked) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)
    } else {
        if (isLocked) Color(0xFF1E2536) else Color(0xFF28324A)
    }
    val titleColor = if (isGameMode) Color(0xFF0F172A) else Color.White
    val subtitleColor = if (isGameMode) Color(0xFF334155) else Color(0xFFE2E8F0)
    val mutedColor = if (isGameMode) Color(0xFF64748B) else Color(0xFF94A3B8)

    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.05f) })
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isGameMode && !isLocked) 2.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isLocked) {
                    onLockedClick()
                } else {
                    slide.onAction()
                }
            }
            .testTag("workout_list_item_${slide.drillName.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura izquierda del juego
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = slide.imageResId),
                    contentDescription = slide.drillName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (isLocked) grayscaleFilter else null
                )

                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información central y completa
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slide.difficulty,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLocked) mutedColor else secondaryAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${slide.duration}",
                        fontSize = 9.5.sp,
                        color = mutedColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = slide.drillName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Black,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = slide.subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                    color = subtitleColor
                )

                if (isLocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGameMode) Color(0xFFE2E8F0) else Color(0xFF1E2536))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = mutedColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${slide.requiredXp} XP para desbloquear",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = mutedColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo interactivo para explicar el desbloqueo de entrenamientos bloqueados
 */
@Composable
private fun LockedWorkoutDialog(
    slide: HeroWorkoutSlide,
    playerTotalXp: Int,
    isGameMode: Boolean,
    primaryAccent: Color,
    onDismiss: () -> Unit
) {
    val xpNeeded = (slide.requiredXp - playerTotalXp).coerceAtLeast(0)
    val progress = if (slide.requiredXp > 0) {
        (playerTotalXp.toFloat() / slide.requiredXp.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val dialogBg = if (isGameMode) Color.White else Color(0xFF141926)
    val titleColor = if (isGameMode) Color(0xFF0F172A) else Color.White
    val bodyColor = if (isGameMode) Color(0xFF334155) else Color(0xFFCBD5E1)
    val subtitleColor = if (isGameMode) Color(0xFF64748B) else Color(0xFF94A3B8)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isGameMode) Color(0xFFF1F5F9) else Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = slide.drillName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = titleColor
                    )
                    Text(
                        text = "Entrenamiento bloqueado (Nivel ${slide.levelNumber})",
                        fontSize = 11.sp,
                        color = subtitleColor
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = slide.subtitle,
                    fontSize = 13.sp,
                    color = bodyColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tu progreso:",
                        fontSize = 11.sp,
                        color = subtitleColor
                    )
                    Text(
                        text = "$playerTotalXp / ${slide.requiredXp} XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = primaryAccent,
                    trackColor = if (isGameMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "¡Juega a los modos activos para ganar los $xpNeeded XP que te faltan y desbloquearlo!",
                    fontSize = 11.5.sp,
                    color = subtitleColor,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    )
}
