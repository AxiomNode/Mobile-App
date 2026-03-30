package es.sebas1705.axiomnode.network

import io.ktor.client.HttpClient

/**
 * Crea un HttpClient con el engine adecuado para cada plataforma.
 */
expect fun createHttpClient(): HttpClient
