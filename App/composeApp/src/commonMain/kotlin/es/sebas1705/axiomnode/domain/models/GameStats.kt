package es.sebas1705.axiomnode.domain.models

/**
 * Estadísticas agregadas del historial de juegos del usuario.
 */
data class GameStats(
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val averageScore: Int,
    val totalPlayTimeSeconds: Int,
) {
    val winRate: Float
        get() = if (totalGames > 0) wins.toFloat() / totalGames else 0f

    val winRatePercentage: Int
        get() = (winRate * 100).toInt()
}

