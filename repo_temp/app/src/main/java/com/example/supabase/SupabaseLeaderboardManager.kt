package com.example.supabase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Representa a un jugador real registrado en Supabase.
 * Solo contiene datos reales: usuario, avatar, puntuación y puesto en el ranking.
 */
data class RealLeaderboardPlayer(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val team: String,
    val score: Int,
    val rank: Int
)

/**
 * Gestor singleton para el Leaderboard y Jugadores Cercanos.
 * - Mantiene los datos reales en memoria (StateFlow) para evitar recargas o parpadeos molestos.
 * - NO inventa jugadores ni nombres falsos. Solo utiliza usuarios registrados en Supabase.
 */
object SupabaseLeaderboardManager {
    private const val TAG = "SupabaseLeaderboardMgr"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _players = MutableStateFlow<List<RealLeaderboardPlayer>>(emptyList())
    val players: StateFlow<List<RealLeaderboardPlayer>> = _players.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    init {
        // Carga inicial al inicializar el objeto
        refreshLeaderboard()
    }

    /**
     * Carga y sincroniza los datos reales de Supabase.
     * Combina perfiles registrados con las puntuaciones de minijuegos.
     */
    fun refreshLeaderboard(force: Boolean = false) {
        if (_isLoading.value && !force) return

        scope.launch {
            _isLoading.value = true
            try {
                // 1. Obtener puntuaciones de minijuegos
                val scoresResult = SupabaseClient.getLeaderboard(limit = 50)
                val scoresList = scoresResult.getOrNull().orEmpty()

                // 2. Obtener perfiles de usuarios registrados
                val profilesResult = SupabaseClient.getRegisteredProfiles(limit = 50)
                val profilesList = profilesResult.getOrNull().orEmpty()

                // Mapear la máxima puntuación por usuario
                val maxScoreByUserId = mutableMapOf<String, Int>()
                val maxScoreByUsername = mutableMapOf<String, Int>()
                scoresList.forEach { item ->
                    if (!item.userId.isNullOrBlank()) {
                        val curr = maxScoreByUserId[item.userId] ?: 0
                        if (item.score > curr) maxScoreByUserId[item.userId] = item.score
                    }
                    if (item.username.isNotBlank()) {
                        val curr = maxScoreByUsername[item.username.trim().lowercase()] ?: 0
                        if (item.score > curr) maxScoreByUsername[item.username.trim().lowercase()] = item.score
                    }
                }

                val seenIds = mutableSetOf<String>()
                val list = mutableListOf<RealLeaderboardPlayer>()

                // Procesar cada perfil registrado real
                profilesList.forEach { profile ->
                    if (profile.id.isNotBlank() && !seenIds.contains(profile.id)) {
                        seenIds.add(profile.id)
                        val uname = profile.username.trim().ifBlank { "Jugador" }
                        val score = maxScoreByUserId[profile.id]
                            ?: maxScoreByUsername[uname.lowercase()]
                            ?: 0

                        list.add(
                            RealLeaderboardPlayer(
                                userId = profile.id,
                                username = uname,
                                avatarUrl = profile.avatarUrl,
                                team = profile.team.orEmpty(),
                                score = score,
                                rank = 0
                            )
                        )
                    }
                }

                // Asegurar usuarios con puntuación no incluidos en profiles
                scoresList.forEach { scoreItem ->
                    val uid = scoreItem.userId.orEmpty()
                    if (uid.isNotBlank() && !seenIds.contains(uid)) {
                        seenIds.add(uid)
                        list.add(
                            RealLeaderboardPlayer(
                                userId = uid,
                                username = scoreItem.username.trim().ifBlank { "Jugador" },
                                avatarUrl = scoreItem.avatarUrl,
                                team = scoreItem.team,
                                score = scoreItem.score,
                                rank = 0
                            )
                        )
                    }
                }

                // Ordenar por puntuación descendente y asignar ranking oficial
                list.sortWith(
                    compareByDescending<RealLeaderboardPlayer> { it.score }
                        .thenBy { it.username.lowercase() }
                )

                val rankedList = list.mapIndexed { index, player ->
                    player.copy(rank = index + 1)
                }

                _players.value = rankedList
                _hasLoaded.value = true
                Log.d(TAG, "Leaderboard actualizado con ${rankedList.size} jugadores reales de Supabase.")
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando leaderboard de Supabase", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
