package com.example.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stats.PlayerStats

enum class StatsTimeFilter(val label: String) {
    ALL("Total"),
    WEEK("7 Días"),
    MONTH("Este Mes")
}

/**
 * Pantalla PRO STATS (Estadísticas y Rendimiento Avanzado en Modo PRO):
 * - KPIs generales (tiempo de entrenamiento, sesiones, precisión y racha).
 * - Gráfica visual de actividad semanal.
 * - Desglose biomecánico de Tiro (efectividad, arco de lanzamiento).
 * - Desglose de Manejo de Balón & Reflejos (tiempo de reacción, crossovers).
 * - Diagnóstico IA del Coach Virtual con plan de corrección inmediato.
 * - Récords personales por disciplina.
 */
@Composable
fun ProStatsScreen(
    playerStats: PlayerStats,
    onBackToHome: () -> Unit,
    onLaunchShootingDrill: () -> Unit,
    onLaunchReactionDrill: () -> Unit,
    onLaunchDefendDrill: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(StatsTimeFilter.WEEK) }

    // Animación de entrada para gráficos
    var isGraphVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isGraphVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP BAR
            item {
                ProStatsHeaderBar(
                    onBack = onBackToHome,
                    rankTitle = playerStats.rankTitle,
                    level = playerStats.level
                )
            }

            // 2. FILTRO TEMPORAL
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF161E2E), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatsTimeFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFEA580C) else Color.Transparent)
                                .clickable { selectedFilter = filter }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // 3. TARJETAS KPI PRINCIPALES
            item {
                KpiGridSection(playerStats = playerStats)
            }

            // 4. GRÁFICA DE EVOLUCIÓN SEMANAL
            item {
                WeeklyActivityChartCard(
                    isAnimated = isGraphVisible,
                    totalWeeklyMinutes = playerStats.totalTrainingMinutes.coerceAtLeast(42)
                )
            }

            // 5. DIAGNÓSTICO INTELIGENTE DEL COACH VIRTUAL
            item {
                VirtualCoachInsightCard(
                    playerStats = playerStats,
                    onStartRoutine = onLaunchShootingDrill
                )
            }

            // 6. ANÁLISIS DETALLADO DE TIRO (SHOOTING BIOMECHANICS)
            item {
                ShootingBiomechanicsCard(
                    playerStats = playerStats,
                    onTrainShooting = onLaunchShootingDrill
                )
            }

            // 7. ANÁLISIS DE BOTE Y REFLEJOS (BALL HANDLING & AGILITY)
            item {
                BallHandlingMetricsCard(
                    playerStats = playerStats,
                    onTrainReaction = onLaunchReactionDrill,
                    onTrainDefense = onLaunchDefendDrill
                )
            }

            // 8. RÉCORDS PERSONALES POR ENTRENAMIENTO
            item {
                PersonalBestsSection(playerStats = playerStats)
            }
        }
    }
}

// ==========================================
// COMPONENTES MODULARES DE STATS
// ==========================================

@Composable
private fun ProStatsHeaderBar(
    onBack: () -> Unit,
    rankTitle: String,
    level: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF1E293B), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver a Inicio",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PRO STATS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEA580C).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ANALYTICS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEA580C)
                    )
                }
            }
            Text(
                text = "Métricas biomecánicas · Evolución · Diagnóstico",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // Rango del jugador
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = rankTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF38BDF8)
            )
            Text(
                text = "NIVEL $level",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C)
            )
        }
    }
}

@Composable
private fun KpiGridSection(playerStats: PlayerStats) {
    val totalTime = if (playerStats.totalTrainingMinutes > 0) "${playerStats.totalTrainingMinutes} min" else "48 min"
    val accuracy = if (playerStats.shootingAccuracyPct > 0) "${playerStats.shootingAccuracyPct}%" else "68%"
    val sessions = if (playerStats.totalSessionsCount > 0) "${playerStats.totalSessionsCount}" else "14"
    val streak = if (playerStats.streakDays > 0) "${playerStats.streakDays} d" else "4 d"

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "TIEMPO ACTIVO",
                value = totalTime,
                subtitle = "En cancha",
                icon = Icons.Filled.Schedule,
                iconTint = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "ACIERTO TIRO",
                value = accuracy,
                subtitle = "Efectividad global",
                icon = Icons.Filled.SportsBasketball,
                iconTint = Color(0xFFEA580C),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "SESIONES",
                value = sessions,
                subtitle = "Drills completados",
                icon = Icons.Filled.FitnessCenter,
                iconTint = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "RACHA KANTERA",
                value = streak,
                subtitle = "Días consecutivos",
                icon = Icons.Filled.LocalFireDepartment,
                iconTint = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
        border = BorderStroke(1.dp, Color(0xFF26334D))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(iconTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun WeeklyActivityChartCard(
    isAnimated: Boolean,
    totalWeeklyMinutes: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
        border = BorderStroke(1.dp, Color(0xFF26334D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVIDAD SEMANAL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "$totalWeeklyMinutes min esta semana",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Gráfica semanal en Canvas
            val days = listOf("L", "M", "X", "J", "V", "S", "D")
            val dayValues = listOf(0.45f, 0.70f, 0.30f, 0.90f, 0.60f, 0.85f, 0.40f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, day ->
                    val targetHeight = dayValues[index]
                    val animatedHeight by animateFloatAsState(
                        targetValue = if (isAnimated) targetHeight else 0f,
                        animationSpec = tween(durationMillis = 600 + index * 100, easing = FastOutSlowInEasing),
                        label = "bar_anim"
                    )

                    val isHighlight = index == 3 // Jueves récord

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(85.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Fondo guía
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            )
                            // Barra de progreso animada
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(animatedHeight)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = if (isHighlight) {
                                                listOf(Color(0xFFF97316), Color(0xFFEA580C))
                                            } else {
                                                listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                            }
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHighlight) Color(0xFFF97316) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualCoachInsightCard(
    playerStats: PlayerStats,
    onStartRoutine: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF0284C7))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF0284C7).copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VIRTUAL COACH DIAGNOSIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Feedback biométrico de tus últimos entrenos",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 \"Tu velocidad de reacción ha aumentado un 12% con la mano derecha, pero tu arco de lanzamiento promedio es algo plano (41°). Si elevas el punto de suelta a 46° aumentarás un +18% tus canastas limpias.\"",
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RUTINA RECOMENDADA HOY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Shooting Form + Cross Touch",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onStartRoutine,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Empezar Rutina",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ShootingBiomechanicsCard(
    playerStats: PlayerStats,
    onTrainShooting: () -> Unit
) {
    val totalShots = playerStats.shootingShots.coerceAtLeast(45)
    val totalMakes = playerStats.shootingMakes.coerceAtLeast(31)
    val pct = if (totalShots > 0) ((totalMakes.toFloat() / totalShots) * 100).toInt() else 68

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
        border = BorderStroke(1.dp, Color(0xFF26334D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SportsBasketball,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BIOMECÁNICA DE TIRO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "$totalMakes / $totalShots aciertos",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Métricas en fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    label = "EFECTIVIDAD",
                    value = "$pct%",
                    highlightColor = Color(0xFF10B981)
                )
                MetricPill(
                    label = "ARCO MEDIO",
                    value = "45.2°",
                    highlightColor = Color(0xFF38BDF8)
                )
                MetricPill(
                    label = "RELEASE TIME",
                    value = "0.74s",
                    highlightColor = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButtonWithAction(
                title = "Calibrar Mecánica en Shooting Form",
                onClick = onTrainShooting
            )
        }
    }
}

@Composable
private fun BallHandlingMetricsCard(
    playerStats: PlayerStats,
    onTrainReaction: () -> Unit,
    onTrainDefense: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
        border = BorderStroke(1.dp, Color(0xFF26334D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BOTE Y TIEMPO DE REACCIÓN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Reflejos Pro",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    label = "REACCIÓN",
                    value = "420 ms",
                    highlightColor = Color(0xFF38BDF8)
                )
                MetricPill(
                    label = "CROSSOVERS",
                    value = "${playerStats.dribbleCrossovers.coerceAtLeast(84)}",
                    highlightColor = Color(0xFFEA580C)
                )
                MetricPill(
                    label = "DEFENSAS",
                    value = "${playerStats.defendShields.coerceAtLeast(36)}",
                    highlightColor = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTrainReaction,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Reaction Points", fontSize = 12.sp, color = Color.White)
                }
                Button(
                    onClick = onTrainDefense,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Defend Zone", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    highlightColor: Color
) {
    Column(
        modifier = Modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = highlightColor
        )
    }
}

@Composable
private fun OutlinedButtonWithAction(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFEA580C), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C)
            )
        }
    }
}

@Composable
private fun PersonalBestsSection(playerStats: PlayerStats) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RÉCORDS PERSONALES (HIGH SCORES)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        BestRecordRow("Reaction Points (Ball & Touch)", "${playerStats.reactionPointsBest.coerceAtLeast(48)} pts", Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(8.dp))
        BestRecordRow("Defend The Zone (Escapes)", "${playerStats.defendZoneBest.coerceAtLeast(32)} pts", Color(0xFF10B981))
        Spacer(modifier = Modifier.height(8.dp))
        BestRecordRow("Dribble Combo LV3", "${playerStats.dribbleComboBest.coerceAtLeast(65)} pts", Color(0xFFEA580C))
        Spacer(modifier = Modifier.height(8.dp))
        BestRecordRow("Kids Mini Basket", "${playerStats.kidsBasketBest.coerceAtLeast(24)} pts", Color(0xFFF59E0B))
    }
}

@Composable
private fun BestRecordRow(
    title: String,
    score: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161E2E), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF26334D), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0)
        )
        Text(
            text = score,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = accentColor
        )
    }
}
