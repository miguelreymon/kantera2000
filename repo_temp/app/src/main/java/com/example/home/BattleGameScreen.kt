package com.example.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.stats.PlayerStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Secciones de Battle Zone en modo GAME
 */
enum class BattleSubTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CHALLENGES("1 vs 1 & Retos", Icons.Filled.Bolt),
    BLUETOOTH("Bluetooth Cancha", Icons.Filled.Bluetooth),
    LEAGUES("Mi Liga", Icons.Filled.Group)
}

/**
 * Juegos disponibles para duelo con sus recursos gráficos reales
 */
enum class BattleDrillType(
    val title: String,
    val category: String,
    val duration: String,
    val imageResId: Int
) {
    BALL_TOUCH("Ball & Touch", "Reflejos", "2 MIN", R.drawable.point),
    CROSS_TOUCH("Cross Touch", "Coordinación", "2 MIN", R.drawable.kantera_point),
    DEFEND_ZONE("Defend Zone", "Defensa", "1 MIN", R.drawable.kantera_mano),
    LASER_ZONE("Laser Zone", "Agilidad", "1 MIN", R.drawable.kantera_laser),
    SHOOTING("Shooting Form", "Tiro", "3 MIN", R.drawable.kantera_tiro),
    KIDS_BASKET("Kids Mini Basket", "Aciertos", "1 MIN", R.drawable.kantera_kid)
}

data class NearbyPeer(
    val id: String,
    val name: String,
    val deviceType: String,
    val distanceMeters: Int
)

data class LeagueMember(
    val rank: Int,
    val name: String,
    val score: Int,
    val wins: Int,
    val isCurrentUser: Boolean = false
)

/**
 * Pantalla BATTLE ZONE con el diseño idéntico del ecosistema GAME:
 * - Fondo blanco limpio
 * - Misma cabecera WorkoutHeaderRow con Avatar, Racha, XP y Toggle GAME / PRO
 * - Tarjetas luminosas con sombras suaves, imágenes reales y acentos cyan y naranja
 * - Duelos 1 vs 1, radar Bluetooth local y Liga privada de amigos
 */
@Composable
fun BattleGameScreen(
    playerName: String,
    avatarUrl: String?,
    isGameMode: Boolean,
    playerStats: PlayerStats,
    onToggleMode: (Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onOpenXpBreakdown: () -> Unit,
    onBackToHome: () -> Unit,
    onLaunchReactionPoints: () -> Unit,
    onLaunchDefendZone: () -> Unit,
    onLaunchShooting: () -> Unit,
    onLaunchKidsBasket: () -> Unit,
    onLaunchDribble: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedSubTab by remember { mutableStateOf(BattleSubTab.CHALLENGES) }
    var selectedDrill by remember { mutableStateOf(BattleDrillType.BALL_TOUCH) }

    // Generador de códigos
    var challengeCode by remember { mutableStateOf("BASKET-782") }
    var showCopyFeedback by remember { mutableStateOf(false) }

    // Matchmaking aleatorio
    var isSearchingOpponent by remember { mutableStateOf(false) }
    var randomOpponentFound by remember { mutableStateOf<String?>(null) }

    // Bluetooth Local
    var isBluetoothVisible by remember { mutableStateOf(true) }
    var isScanningBluetooth by remember { mutableStateOf(false) }
    val nearbyPeers = remember {
        mutableStateListOf(
            NearbyPeer("p1", "Tablet de Lucas", "Tablet (Sin SIM)", 3),
            NearbyPeer("p2", "Pablo_Court", "Móvil Cancha", 5),
            NearbyPeer("p3", "Cadete Marcos", "iPad Club", 7)
        )
    }

    // Ligas y Equipos
    var leagueName by remember { mutableStateOf("Los Reyes del Parque") }
    var leagueCode by remember { mutableStateOf("REYES-2026") }
    var showCreateLeagueDialog by remember { mutableStateOf(false) }
    var newLeagueInput by remember { mutableStateOf("") }
    val leagueMembers = remember {
        mutableStateListOf(
            LeagueMember(1, "Marc_99", 540, 12),
            LeagueMember(2, playerName.ifBlank { "Tú (Miguel)" }, 490, 10, isCurrentUser = true),
            LeagueMember(3, "Carlos Dribble", 420, 8),
            LeagueMember(4, "Hugo Splash", 380, 7)
        )
    }

    val launchDrillForType: (BattleDrillType) -> Unit = { type ->
        when (type) {
            BattleDrillType.BALL_TOUCH, BattleDrillType.CROSS_TOUCH -> onLaunchReactionPoints()
            BattleDrillType.DEFEND_ZONE, BattleDrillType.LASER_ZONE -> onLaunchDefendZone()
            BattleDrillType.SHOOTING -> onLaunchShooting()
            BattleDrillType.KIDS_BASKET -> onLaunchKidsBasket()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. CABECERA SUPERIOR IDÉNTICA A WORKOUT Y HOME
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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2. HERO BANNER BATTLE
            item {
                BattleHeroBanner(totalWins = 14)
            }

            // 3. PESTAÑAS SELECTORAS (Estilo Chips del Modo GAME)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BattleSubTab.values().forEach { tab ->
                        val isSelected = selectedSubTab == tab
                        val bgCol by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF2FB2C9) else Color.Transparent,
                            label = "tab_bg"
                        )
                        val textCol by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color(0xFF64748B),
                            label = "tab_txt"
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgCol)
                                .clickable { selectedSubTab = tab }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = textCol,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = textCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. CONTENIDO SEGÚN SUBTAB SELECCIONADA
            when (selectedSubTab) {
                BattleSubTab.CHALLENGES -> {
                    // Selector visual de juego para el reto
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ELIGE EL JUEGO PARA EL RETO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = selectedDrill.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2FB2C9)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(BattleDrillType.values()) { drill ->
                                    val isSelected = selectedDrill == drill
                                    Card(
                                        modifier = Modifier
                                            .width(135.dp)
                                            .clickable { selectedDrill = drill },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) Color(0xFF2FB2C9) else Color(0xFFE2E8F0)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = drill.imageResId),
                                                    contentDescription = drill.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    Color.Transparent,
                                                                    Color.Black.copy(alpha = 0.5f)
                                                                )
                                                            )
                                                        )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(6.dp)
                                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = drill.duration,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = drill.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = drill.category,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta: Invitar Amigo Directo
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFF2FB2C9).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = null,
                                            tint = Color(0xFF0F869B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Invitar a un Amigo",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Envíale el reto por WhatsApp o comparte tu código",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Código de reto
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "CÓDIGO DE RETO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = challengeCode,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F869B),
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            challengeCode = "BASKET-" + (100..999).random()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "Nuevo código",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Código Kantera", challengeCode))
                                            showCopyFeedback = true
                                            coroutineScope.launch {
                                                delay(2000)
                                                showCopyFeedback = false
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (showCopyFeedback) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                                contentDescription = "Copiar",
                                                tint = if (showCopyFeedback) Color(0xFF10B981) else Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "🔥 ¡Te he retado a un duelo en Kantera Basketball!\n" +
                                                            "Juego: ${selectedDrill.title}\n" +
                                                            "Código de reto: $challengeCode\n" +
                                                            "¿Te atreves a superar mi puntuación?"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Enviar reto a un amigo"))
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2FB2C9)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F869B))
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compartir", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { launchDrillForType(selectedDrill) },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("¡Jugar Duelo!", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta: Rival Aleatorio
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFD93B98).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFFD93B98),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Buscar Rival Aleatorio",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Emparejamiento rápido con un jugador de tu nivel",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isSearchingOpponent) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFDF2F8), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFFFCE7F3), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                        val scale by infiniteTransition.animateFloat(
                                            initialValue = 0.9f,
                                            targetValue = 1.15f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "scale"
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = null,
                                            tint = Color(0xFFD93B98),
                                            modifier = Modifier
                                                .size(20.dp)
                                                .scale(scale)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Buscando jugador compatible...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD93B98)
                                        )
                                    }
                                } else if (randomOpponentFound != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFDF2F8), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFFF472B6), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "¡OPONENTE ENCONTRADO!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFBE185D)
                                        )
                                        Text(
                                            text = randomOpponentFound ?: "",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { launchDrillForType(selectedDrill) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD93B98))
                                        ) {
                                            Text("¡Comenzar Duelo Ahora!", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isSearchingOpponent = true
                                            coroutineScope.launch {
                                                delay(2000)
                                                isSearchingOpponent = false
                                                randomOpponentFound = "Alex_Bball (Nivel 2 · 840 Hype)"
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                                    ) {
                                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Buscar Rival Aleatorio", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                BattleSubTab.BLUETOOTH -> {
                    // Banner explicativo Offline
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA)),
                            border = BorderStroke(1.dp, Color(0xFF80DEEA))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF00ACC1), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WifiOff,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MODO CANCHA 100% OFFLINE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF006064)
                                    )
                                    Text(
                                        text = "Conecta con amigos cercanos por Bluetooth sin consumir datos móviles ni necesitar WiFi.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF00838F),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Switch Visibilidad
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Visible a otros jugadores",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isBluetoothVisible) "Tu tablet/móvil aparece en el radar" else "Modo oculto",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = isBluetoothVisible,
                                    onCheckedChange = { isBluetoothVisible = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF2FB2C9)
                                    )
                                )
                            }
                        }
                    }

                    // Lista de rivales cercanos
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RIVALES CERCA EN LA CANCHA (${nearbyPeers.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Button(
                                onClick = {
                                    isScanningBluetooth = true
                                    coroutineScope.launch {
                                        delay(2000)
                                        isScanningBluetooth = false
                                        Toast.makeText(context, "Radar actualizado: 3 dispositivos listos", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isScanningBluetooth) "Buscando..." else "Escanear Radar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    items(nearbyPeers) { peer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFFE0F2FE), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bluetooth,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = peer.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${peer.deviceType} · ${peer.distanceMeters}m de ti",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Reto enviado a ${peer.name} por Bluetooth", Toast.LENGTH_SHORT).show()
                                        launchDrillForType(selectedDrill)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Retar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                BattleSubTab.LEAGUES -> {
                    // Tarjeta Liga Activa
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color(0xFFFEF3C7), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.EmojiEvents,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "LIGA PRIVADA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706)
                                            )
                                            Text(
                                                text = leagueName,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { showCreateLeagueDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Text("Crear / Unirse", fontSize = 11.sp, color = Color(0xFF334155))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "CÓDIGO DE ACCESO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                        Text(text = leagueCode, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                    Button(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "🏀 ¡Únete a nuestra liga '$leagueName' en Kantera Basketball!\n" +
                                                            "Código: $leagueCode"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Invitar a la liga"))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Invitar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Ranking de amigos en la liga
                    item {
                        Text(
                            text = "TABLA DE CLASIFICACIÓN DE LA LIGA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(leagueMembers) { member ->
                        val isCurrentUser = member.isCurrentUser
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentUser) Color(0xFFEFF6FF) else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentUser) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Puesto
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(
                                            when (member.rank) {
                                                1 -> Color(0xFFF59E0B)
                                                2 -> Color(0xFF94A3B8)
                                                3 -> Color(0xFFB45309)
                                                else -> Color(0xFFE2E8F0)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${member.rank}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (member.rank <= 3) Color.White else Color(0xFF334155)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrentUser) FontWeight.Black else FontWeight.Bold,
                                        color = if (isCurrentUser) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${member.wins} victorias en duelos",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Text(
                                    text = "${member.score} pts",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Crear Liga
    if (showCreateLeagueDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showCreateLeagueDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Crear o Unirse a una Liga",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Introduce el nombre del grupo de tus amigos o club.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newLeagueInput,
                        onValueChange = { newLeagueInput = it },
                        placeholder = { Text("Nombre o Código de Liga") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2FB2C9),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showCreateLeagueDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (newLeagueInput.isNotBlank()) {
                                    leagueName = newLeagueInput
                                    leagueCode = newLeagueInput.uppercase().take(6) + "-2026"
                                    showCreateLeagueDialog = false
                                    newLeagueInput = ""
                                    Toast.makeText(context, "¡Liga lista!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2FB2C9))
                        ) {
                            Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleHeroBanner(totalWins: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner1),
                contentDescription = "Battle Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradiente sobre la imagen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.40f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VS 1v1 BATTLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Whatshot, contentDescription = null, tint = Color(0xFFFF6B1A), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$totalWins Victorias",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "DESAFÍA A TUS AMIGOS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "Compite en reflejos, bote y tiro cara a cara",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
