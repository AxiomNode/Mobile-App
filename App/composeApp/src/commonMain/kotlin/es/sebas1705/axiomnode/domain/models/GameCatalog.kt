package es.sebas1705.axiomnode.domain.models

/**
 * Categorias y lenguajes disponibles para juegos.
 */
data class GameCategory(
    val id: String,
    val name: String,
)

data class GameLanguage(
    val code: String, // e.g. "es", "en"
    val name: String,
)

data class GameCatalog(
    val categories: List<GameCategory>,
    val languages: List<GameLanguage>,
)

