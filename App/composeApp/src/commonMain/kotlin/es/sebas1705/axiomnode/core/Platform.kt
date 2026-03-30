package es.sebas1705.axiomnode.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform