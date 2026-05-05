package es.sebas1705.axiomnode.presentation.navigation

import es.sebas1705.axiomnode.domain.models.GameType

/**
 * Top-level destinations shown in the bottom navigation bar / rail.
 */
enum class TopDestination(val title: String) {
    HOME("Inicio"),
    CATALOG("Catálogo"),
    HISTORY("Historial"),
    STATS("Stats"),
    PROFILE("Perfil"),
}

/**
 * All in-app destinations (top-level + sub-screens stacked above the bottom bar).
 */
sealed interface Destination {
    /** A top-level tab destination. */
    data class Top(val tab: TopDestination) : Destination

    /** Generate-game lobby. */
    data class Lobby(val gameType: GameType) : Destination

    /** Active game playback. Game is resolved by id from cache/state. */
    data class Play(val gameId: String) : Destination

    /** Read-only history detail (shows the cached game + result). */
    data class HistoryDetail(val gameId: String, val resultTimestamp: Long) : Destination

    data object Settings : Destination
    data object About : Destination
}

