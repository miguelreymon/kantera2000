package com.example.stats

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fuente detallada de generación de puntos XP por cada juego/disciplina.
 */
data class GameXpSource(
    val gameId: String,
    val gameName: String,
    val category: String,
    val xpPoints: Int,
    val percentageOfTotal: Float,
    val sessionsOrGames: Int,
    val detailText: String,
    val emoji: String,
    val colorHex: Long
)

/**
 * Modelo de datos inmutable para las estadísticas y progreso del usuario.
 */
data class PlayerStats(
    val userId: String = "",
    val playerName: String = "Miguel",
    // 1. Minijuego: Reaction Points
    val reactionPointsScore: Int = 0,
    val reactionPointsBest: Int = 0,
    val reactionPointsGames: Int = 0,
    val reactionPointsHits: Int = 0,
    // 2. Minijuego: Dribble Combo LV3
    val dribbleComboScore: Int = 0,
    val dribbleComboBest: Int = 0,
    val dribbleComboGames: Int = 0,
    val dribbleCrossovers: Int = 0,
    // 3. Minijuego: Defend The Zone
    val defendZoneScore: Int = 0,
    val defendZoneBest: Int = 0,
    val defendZoneGames: Int = 0,
    val defendShields: Int = 0,
    // 4. Minijuego: Kids Mini Basket Arcade
    val kidsBasketScore: Int = 0,
    val kidsBasketBest: Int = 0,
    val kidsBasketGames: Int = 0,
    val kidsBasketMakes: Int = 0,
    // 5. Sesiones de Tiro a Canasta
    val shootingShots: Int = 0,
    val shootingMakes: Int = 0,
    val shootingSessions: Int = 0,
    val shootingXp: Int = 0,
    // Métrica de tiempo y racha
    val totalTrainingMinutes: Int = 0,
    val streakDays: Int = 0,
    val lastActiveDate: String? = null,
    val avatarUrl: String? = null
) {
    // Control total de XP generado por juego
    val xpShooting: Int
        get() = shootingXp

    val xpReactionPoints: Int
        get() = reactionPointsScore

    val xpDribbleCombo: Int
        get() = dribbleComboScore

    val xpDefendZone: Int
        get() = defendZoneScore

    val xpKidsMiniBasket: Int
        get() = kidsBasketScore

    val totalGamePoints: Int
        get() = xpReactionPoints + xpDribbleCombo + xpDefendZone + xpKidsMiniBasket

    val totalXp: Int
        get() = totalGamePoints + xpShooting

    // Precisión de tiro
    val shootingAccuracyPct: Int
        get() = if (shootingShots > 0) ((shootingMakes.toFloat() / shootingShots) * 100).toInt() else 0

    // Entrenamientos totales realizados
    val totalMinigamesCount: Int
        get() = reactionPointsGames + dribbleComboGames + defendZoneGames + kidsBasketGames

    val totalSessionsCount: Int
        get() = shootingSessions + totalMinigamesCount

    // Sistema robusto de Niveles (Nivel 1 al crear cuenta con 0 XP)
    val level: Int
        get() = when {
            totalXp < 50 -> 1
            totalXp < 120 -> 2
            totalXp < 200 -> 3
            totalXp < 300 -> 4
            totalXp < 450 -> 5
            totalXp < 600 -> 6
            totalXp < 800 -> 7
            totalXp < 1050 -> 8
            totalXp < 1350 -> 9
            totalXp < 1700 -> 10
            totalXp < 2100 -> 11
            totalXp < 2600 -> 12
            totalXp < 3200 -> 13
            totalXp < 4000 -> 14
            else -> 15 + ((totalXp - 4000) / 800)
        }

    // Rangos de Liga
    val rankTitle: String
        get() = when {
            totalXp >= 3000 -> "HALL OF FAME"
            totalXp >= 1500 -> "MVP"
            totalXp >= 750 -> "ALL-STAR"
            totalXp >= 250 -> "PRO"
            else -> "ROOKIE"
        }

    val rankSubtitle: String
        get() = when (rankTitle) {
            "HALL OF FAME" -> "Leyenda Kantera"
            "MVP" -> "Jugador Más Valioso"
            "ALL-STAR" -> "Élite del Baloncesto"
            "PRO" -> "Jugador Profesional"
            else -> "Novato en Desarrollo"
        }

    val nextRankXp: Int
        get() = when {
            totalXp >= 3000 -> 5000
            totalXp >= 1500 -> 3000
            totalXp >= 750 -> 1500
            totalXp >= 250 -> 750
            else -> 250
        }

    val currentRankBaseXp: Int
        get() = when {
            totalXp >= 3000 -> 3000
            totalXp >= 1500 -> 1500
            totalXp >= 750 -> 750
            totalXp >= 250 -> 250
            else -> 0
        }

    val rankProgress: Float
        get() {
            val range = (nextRankXp - currentRankBaseXp).coerceAtLeast(1)
            val currentInRange = (totalXp - currentRankBaseXp).coerceAtLeast(0)
            return (currentInRange.toFloat() / range.toFloat()).coerceIn(0f, 1f)
        }

    val xpToNextRank: Int
        get() = (nextRankXp - totalXp).coerceAtLeast(0)

    /**
     * Devuelve el desglose exhaustivo de los puntos XP originados en cada uno de los juegos.
     */
    fun getXpBreakdown(): List<GameXpSource> {
        val total = totalXp.coerceAtLeast(1).toFloat()
        return listOf(
            GameXpSource(
                gameId = "SHOOTING",
                gameName = "Sesiones de Tiro",
                category = "Entrenamiento de Puntería",
                xpPoints = xpShooting,
                percentageOfTotal = (xpShooting.toFloat() / total) * 100f,
                sessionsOrGames = shootingSessions,
                detailText = "$shootingMakes canastas de $shootingShots tiros • $shootingSessions sesiones",
                emoji = "🏀",
                colorHex = 0xFFF97316 // Naranja Baloncesto
            ),
            GameXpSource(
                gameId = "REACTION_POINTS",
                gameName = "Reaction Points",
                category = "Bote y Velocidad de Reacción",
                xpPoints = xpReactionPoints,
                percentageOfTotal = (xpReactionPoints.toFloat() / total) * 100f,
                sessionsOrGames = reactionPointsGames,
                detailText = "Récord: $reactionPointsBest pts • $reactionPointsHits toques registrados",
                emoji = "⚡",
                colorHex = 0xFF06B6D4 // Cyan
            ),
            GameXpSource(
                gameId = "DRIBBLE_COMBO",
                gameName = "Dribble Combo LV3",
                category = "Manejo y Ritmo de Bote",
                xpPoints = xpDribbleCombo,
                percentageOfTotal = (xpDribbleCombo.toFloat() / total) * 100f,
                sessionsOrGames = dribbleComboGames,
                detailText = "Récord: $dribbleComboBest pts • $dribbleCrossovers crossovers logrados",
                emoji = "🤹",
                colorHex = 0xFF8B5CF6 // Púrpura
            ),
            GameXpSource(
                gameId = "DEFEND_ZONE",
                gameName = "Defend The Zone",
                category = "Defensa Fantasma y Agilidad",
                xpPoints = xpDefendZone,
                percentageOfTotal = (xpDefendZone.toFloat() / total) * 100f,
                sessionsOrGames = defendZoneGames,
                detailText = "Récord: $defendZoneBest pts • $defendShields escudos protegidos",
                emoji = "🛡️",
                colorHex = 0xFF10B981 // Verde Esmeralda
            ),
            GameXpSource(
                gameId = "KIDS_MINI_BASKET",
                gameName = "Kids Mini Basket",
                category = "Arcade Minibasket Infantil",
                xpPoints = xpKidsMiniBasket,
                percentageOfTotal = (xpKidsMiniBasket.toFloat() / total) * 100f,
                sessionsOrGames = kidsBasketGames,
                detailText = "Récord: $kidsBasketBest pts • $kidsBasketMakes canastas encestadas",
                emoji = "🎯",
                colorHex = 0xFFD93B98 // Rosa Arcade / Magenta
            )
        )
    }
}

object PlayerStatsManager {
    private const val PREFS_STATS = "player_stats_prefs"
    private const val KEY_CURRENT_USER_ID = "key_current_user_id"
    private const val KEY_PLAYER_NAME = "key_player_name"
    private const val KEY_AVATAR_URL = "key_avatar_url"

    // Reaction Points
    private const val KEY_REACTION_SCORE = "key_reaction_score"
    private const val KEY_REACTION_BEST = "key_reaction_best"
    private const val KEY_REACTION_GAMES = "key_reaction_games"
    private const val KEY_REACTION_HITS = "key_reaction_hits"

    // Dribble Combo
    private const val KEY_DRIBBLE_SCORE = "key_dribble_score"
    private const val KEY_DRIBBLE_BEST = "key_dribble_best"
    private const val KEY_DRIBBLE_GAMES = "key_dribble_games"
    private const val KEY_DRIBBLE_CROSSOVERS = "key_dribble_crossovers"

    // Defend Zone
    private const val KEY_DEFEND_SCORE = "key_defend_score"
    private const val KEY_DEFEND_BEST = "key_defend_best"
    private const val KEY_DEFEND_GAMES = "key_defend_games"
    private const val KEY_DEFEND_SHIELDS = "key_defend_shields"

    // Kids Mini Basket
    private const val KEY_KIDS_BASKET_SCORE = "key_kids_basket_score"
    private const val KEY_KIDS_BASKET_BEST = "key_kids_basket_best"
    private const val KEY_KIDS_BASKET_GAMES = "key_kids_basket_games"
    private const val KEY_KIDS_BASKET_MAKES = "key_kids_basket_makes"

    // Tiro a canasta
    private const val KEY_SHOOTING_SHOTS = "key_shooting_shots"
    private const val KEY_SHOOTING_MAKES = "key_shooting_makes"
    private const val KEY_SHOOTING_SESSIONS = "key_shooting_sessions"
    private const val KEY_SHOOTING_XP = "key_shooting_xp"

    // Tiempo y rachas
    private const val KEY_TOTAL_TRAINING_MINUTES = "key_total_training_minutes"
    private const val KEY_STREAK_DAYS = "key_streak_days"
    private const val KEY_LAST_ACTIVE_DATE = "key_last_active_date"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var activeUserId: String = ""

    private val _stats = MutableStateFlow(PlayerStats())
    val stats: StateFlow<PlayerStats> = _stats.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            prefs = appContext?.getSharedPreferences(PREFS_STATS, Context.MODE_PRIVATE)
            loadStats()
            checkAndUpdateStreak()
        }
    }

    private fun getPrefsForUser(userId: String): SharedPreferences {
        val ctx = appContext ?: throw IllegalStateException("PlayerStatsManager no inicializado")
        val name = if (userId.isNotBlank()) "player_stats_$userId" else PREFS_STATS
        return ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    private fun loadStats() {
        val p = prefs ?: return
        _stats.value = PlayerStats(
            userId = activeUserId,
            playerName = p.getString(KEY_PLAYER_NAME, "Miguel") ?: "Miguel",
            reactionPointsScore = p.getInt(KEY_REACTION_SCORE, 0),
            reactionPointsBest = p.getInt(KEY_REACTION_BEST, 0),
            reactionPointsGames = p.getInt(KEY_REACTION_GAMES, 0),
            reactionPointsHits = p.getInt(KEY_REACTION_HITS, 0),
            dribbleComboScore = p.getInt(KEY_DRIBBLE_SCORE, 0),
            dribbleComboBest = p.getInt(KEY_DRIBBLE_BEST, 0),
            dribbleComboGames = p.getInt(KEY_DRIBBLE_GAMES, 0),
            dribbleCrossovers = p.getInt(KEY_DRIBBLE_CROSSOVERS, 0),
            defendZoneScore = p.getInt(KEY_DEFEND_SCORE, 0),
            defendZoneBest = p.getInt(KEY_DEFEND_BEST, 0),
            defendZoneGames = p.getInt(KEY_DEFEND_GAMES, 0),
            defendShields = p.getInt(KEY_DEFEND_SHIELDS, 0),
            kidsBasketScore = p.getInt(KEY_KIDS_BASKET_SCORE, 0),
            kidsBasketBest = p.getInt(KEY_KIDS_BASKET_BEST, 0),
            kidsBasketGames = p.getInt(KEY_KIDS_BASKET_GAMES, 0),
            kidsBasketMakes = p.getInt(KEY_KIDS_BASKET_MAKES, 0),
            shootingShots = p.getInt(KEY_SHOOTING_SHOTS, 0),
            shootingMakes = p.getInt(KEY_SHOOTING_MAKES, 0),
            shootingSessions = p.getInt(KEY_SHOOTING_SESSIONS, 0),
            shootingXp = p.getInt(KEY_SHOOTING_XP, 0),
            totalTrainingMinutes = p.getInt(KEY_TOTAL_TRAINING_MINUTES, 0),
            streakDays = p.getInt(KEY_STREAK_DAYS, 0),
            lastActiveDate = p.getString(KEY_LAST_ACTIVE_DATE, null),
            avatarUrl = p.getString(KEY_AVATAR_URL, null)
        )
    }

    /**
     * Inicializa un nuevo perfil limpio para un usuario recién registrado.
     * Garantiza que empiece desde 0 XP, Rango Rookie, Nivel 1 y 0 días de racha.
     */
    fun onUserAccountCreated(username: String, userId: String? = null) {
        val cleanName = username.trim().ifBlank { "Usuario" }
        val effectiveUserId = userId?.trim().orEmpty()
        activeUserId = effectiveUserId
        prefs = getPrefsForUser(effectiveUserId)

        prefs?.edit()?.apply {
            clear()
            putString(KEY_PLAYER_NAME, cleanName)
            putString(KEY_CURRENT_USER_ID, effectiveUserId)
            putInt(KEY_STREAK_DAYS, 0) // Comienza estrictamente en 0
            remove(KEY_LAST_ACTIVE_DATE)
            putInt(KEY_SHOOTING_SHOTS, 0)
            putInt(KEY_SHOOTING_MAKES, 0)
            putInt(KEY_SHOOTING_SESSIONS, 0)
            putInt(KEY_SHOOTING_XP, 0)
            putInt(KEY_REACTION_SCORE, 0)
            putInt(KEY_REACTION_BEST, 0)
            putInt(KEY_REACTION_GAMES, 0)
            putInt(KEY_REACTION_HITS, 0)
            putInt(KEY_DRIBBLE_SCORE, 0)
            putInt(KEY_DRIBBLE_BEST, 0)
            putInt(KEY_DRIBBLE_GAMES, 0)
            putInt(KEY_DRIBBLE_CROSSOVERS, 0)
            putInt(KEY_DEFEND_SCORE, 0)
            putInt(KEY_DEFEND_BEST, 0)
            putInt(KEY_DEFEND_GAMES, 0)
            putInt(KEY_DEFEND_SHIELDS, 0)
            putInt(KEY_KIDS_BASKET_SCORE, 0)
            putInt(KEY_KIDS_BASKET_BEST, 0)
            putInt(KEY_KIDS_BASKET_GAMES, 0)
            putInt(KEY_KIDS_BASKET_MAKES, 0)
            putInt(KEY_TOTAL_TRAINING_MINUTES, 0)
            apply()
        }
        loadStats()
    }

    /**
     * Carga el perfil aislado de un usuario que ha iniciado sesión.
     */
    fun onUserSignedIn(username: String, userId: String? = null) {
        val cleanName = username.trim().ifBlank { "Usuario" }
        val effectiveUserId = userId?.trim().orEmpty()
        activeUserId = effectiveUserId
        prefs = getPrefsForUser(effectiveUserId)

        if (prefs?.getString(KEY_PLAYER_NAME, null) == null) {
            prefs?.edit()?.putString(KEY_PLAYER_NAME, cleanName)?.apply()
        }
        loadStats()
        checkAndUpdateStreak()
    }

    /**
     * Restablece al cerrar sesión a las preferencias locales por defecto.
     */
    fun onUserSignedOut() {
        activeUserId = ""
        prefs = appContext?.getSharedPreferences(PREFS_STATS, Context.MODE_PRIVATE)
        loadStats()
    }

    fun updatePlayerName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        prefs?.edit()?.putString(KEY_PLAYER_NAME, trimmed)?.apply()
        _stats.value = _stats.value.copy(playerName = trimmed)
    }

    fun updateAvatarUrl(url: String?) {
        val trimmed = url?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            prefs?.edit()?.remove(KEY_AVATAR_URL)?.apply()
            _stats.value = _stats.value.copy(avatarUrl = null)
        } else {
            prefs?.edit()?.putString(KEY_AVATAR_URL, trimmed)?.apply()
            _stats.value = _stats.value.copy(avatarUrl = trimmed)
        }
    }

    /**
     * Sistema de Racha Diaria:
     * - Comprueba la fecha de hoy respecto al último día de actividad registrado.
     * - Si es un día consecutivo (ayer), suma 1 día a la racha.
     * - Si falta un día o más (no entra en la app), la racha se rompe y vuelve a empezar en 1 hoy.
     * - Si es una cuenta nueva con 0 días, pasa a 1 día en su primera entrada.
     */
    fun checkAndUpdateStreak() {
        val p = prefs ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastDateStr = p.getString(KEY_LAST_ACTIVE_DATE, null)
        val currentStreak = p.getInt(KEY_STREAK_DAYS, 0)

        if (lastDateStr == null) {
            // Primer acceso a la app
            val newStreak = if (currentStreak <= 0) 1 else currentStreak
            p.edit()
                .putString(KEY_LAST_ACTIVE_DATE, today)
                .putInt(KEY_STREAK_DAYS, newStreak)
                .apply()
            _stats.value = _stats.value.copy(streakDays = newStreak, lastActiveDate = today)
            return
        }

        if (lastDateStr == today) {
            // Mismo día, racha intacta
            return
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val lastDate = sdf.parse(lastDateStr)
            val todayDate = sdf.parse(today)
            if (lastDate != null && todayDate != null) {
                val diffMillis = todayDate.time - lastDate.time
                val diffDays = (diffMillis / (1000L * 60L * 60L * 24L)).toInt()

                if (diffDays == 1) {
                    // Día consecutivo: se premia la constancia incrementando la racha
                    val newStreak = currentStreak + 1
                    p.edit()
                        .putString(KEY_LAST_ACTIVE_DATE, today)
                        .putInt(KEY_STREAK_DAYS, newStreak)
                        .apply()
                    _stats.value = _stats.value.copy(streakDays = newStreak, lastActiveDate = today)
                } else if (diffDays > 1) {
                    // "si un dia no entra vuelve a empezar"
                    // Racha rota por inactividad: reinicia a 1 día (hoy)
                    val newStreak = 1
                    p.edit()
                        .putString(KEY_LAST_ACTIVE_DATE, today)
                        .putInt(KEY_STREAK_DAYS, newStreak)
                        .apply()
                    _stats.value = _stats.value.copy(streakDays = newStreak, lastActiveDate = today)
                }
            }
        } catch (_: Exception) {
            p.edit().putString(KEY_LAST_ACTIVE_DATE, today).apply()
        }
    }

    /**
     * Registra la puntuación y XP originados en cualquier minijuego.
     */
    fun addMinigameScore(
        gameMode: String,
        score: Int,
        hitsOrCrossovers: Int = 0,
        durationSec: Int = 60
    ) {
        val p = prefs ?: return
        val current = _stats.value
        val addedMinutes = (durationSec / 60).coerceAtLeast(1)
        val newTotalMinutes = current.totalTrainingMinutes + addedMinutes

        val updated = when (gameMode.uppercase()) {
            "REACTION_POINTS" -> {
                val newScore = current.reactionPointsScore + score
                val newBest = maxOf(current.reactionPointsBest, score)
                val newGames = current.reactionPointsGames + 1
                val newHits = current.reactionPointsHits + hitsOrCrossovers
                p.edit()
                    .putInt(KEY_REACTION_SCORE, newScore)
                    .putInt(KEY_REACTION_BEST, newBest)
                    .putInt(KEY_REACTION_GAMES, newGames)
                    .putInt(KEY_REACTION_HITS, newHits)
                    .putInt(KEY_TOTAL_TRAINING_MINUTES, newTotalMinutes)
                    .apply()
                current.copy(
                    reactionPointsScore = newScore,
                    reactionPointsBest = newBest,
                    reactionPointsGames = newGames,
                    reactionPointsHits = newHits,
                    totalTrainingMinutes = newTotalMinutes
                )
            }
            "DRIBBLE_COMBO" -> {
                val newScore = current.dribbleComboScore + score
                val newBest = maxOf(current.dribbleComboBest, score)
                val newGames = current.dribbleComboGames + 1
                val newCross = current.dribbleCrossovers + hitsOrCrossovers
                p.edit()
                    .putInt(KEY_DRIBBLE_SCORE, newScore)
                    .putInt(KEY_DRIBBLE_BEST, newBest)
                    .putInt(KEY_DRIBBLE_GAMES, newGames)
                    .putInt(KEY_DRIBBLE_CROSSOVERS, newCross)
                    .putInt(KEY_TOTAL_TRAINING_MINUTES, newTotalMinutes)
                    .apply()
                current.copy(
                    dribbleComboScore = newScore,
                    dribbleComboBest = newBest,
                    dribbleComboGames = newGames,
                    dribbleCrossovers = newCross,
                    totalTrainingMinutes = newTotalMinutes
                )
            }
            "DEFEND_ZONE" -> {
                val newScore = current.defendZoneScore + score
                val newBest = maxOf(current.defendZoneBest, score)
                val newGames = current.defendZoneGames + 1
                val newShields = current.defendShields + hitsOrCrossovers
                p.edit()
                    .putInt(KEY_DEFEND_SCORE, newScore)
                    .putInt(KEY_DEFEND_BEST, newBest)
                    .putInt(KEY_DEFEND_GAMES, newGames)
                    .putInt(KEY_DEFEND_SHIELDS, newShields)
                    .putInt(KEY_TOTAL_TRAINING_MINUTES, newTotalMinutes)
                    .apply()
                current.copy(
                    defendZoneScore = newScore,
                    defendZoneBest = newBest,
                    defendZoneGames = newGames,
                    defendShields = newShields,
                    totalTrainingMinutes = newTotalMinutes
                )
            }
            "KIDS_MINI_BASKET" -> {
                val newScore = current.kidsBasketScore + score
                val newBest = maxOf(current.kidsBasketBest, score)
                val newGames = current.kidsBasketGames + 1
                val newMakes = current.kidsBasketMakes + hitsOrCrossovers
                p.edit()
                    .putInt(KEY_KIDS_BASKET_SCORE, newScore)
                    .putInt(KEY_KIDS_BASKET_BEST, newBest)
                    .putInt(KEY_KIDS_BASKET_GAMES, newGames)
                    .putInt(KEY_KIDS_BASKET_MAKES, newMakes)
                    .putInt(KEY_TOTAL_TRAINING_MINUTES, newTotalMinutes)
                    .apply()
                current.copy(
                    kidsBasketScore = newScore,
                    kidsBasketBest = newBest,
                    kidsBasketGames = newGames,
                    kidsBasketMakes = newMakes,
                    totalTrainingMinutes = newTotalMinutes
                )
            }
            else -> current
        }
        _stats.value = updated
        checkAndUpdateStreak()
    }

    /**
     * Registra una sesión de tiro a canasta:
     * - Guarda número real de tiros lanzados y anotados.
     * - Asigna 10 XP por canasta anotada + 30 XP de bonificación por sesión completa.
     * - Suma el tiempo de entrenamiento en minutos.
     * - Actualiza la racha de días activos.
     */
    fun addShootingSession(totalShots: Int, makes: Int, durationSec: Int = 0) {
        val p = prefs ?: return
        val current = _stats.value

        val newShots = current.shootingShots + totalShots
        val newMakes = current.shootingMakes + makes
        val newSessions = current.shootingSessions + 1
        val earnedXp = (makes * 10) + 30
        val newShootingXp = current.shootingXp + earnedXp
        val addedMinutes = if (durationSec > 0) (durationSec / 60).coerceAtLeast(1) else if (totalShots > 0) 5 else 0
        val newTotalMinutes = current.totalTrainingMinutes + addedMinutes

        p.edit()
            .putInt(KEY_SHOOTING_SHOTS, newShots)
            .putInt(KEY_SHOOTING_MAKES, newMakes)
            .putInt(KEY_SHOOTING_SESSIONS, newSessions)
            .putInt(KEY_SHOOTING_XP, newShootingXp)
            .putInt(KEY_TOTAL_TRAINING_MINUTES, newTotalMinutes)
            .apply()

        _stats.value = current.copy(
            shootingShots = newShots,
            shootingMakes = newMakes,
            shootingSessions = newSessions,
            shootingXp = newShootingXp,
            totalTrainingMinutes = newTotalMinutes
        )
        checkAndUpdateStreak()
    }

    /**
     * Añade puntos de prueba para verificar rápidamente subida de XP y ranking.
     */
    fun addTestScore(gameMode: String, amount: Int) {
        addMinigameScore(gameMode, amount, hitsOrCrossovers = (amount / 5).coerceAtLeast(1), durationSec = 60)
    }
}

