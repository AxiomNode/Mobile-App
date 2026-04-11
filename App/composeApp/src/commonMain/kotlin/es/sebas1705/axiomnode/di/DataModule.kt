package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.config.createAppConfig
import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import es.sebas1705.axiomnode.data.network.AuthHttpClient
import es.sebas1705.axiomnode.data.network.GameResultSyncEngine
import es.sebas1705.axiomnode.data.network.GamesHttpClient
import es.sebas1705.axiomnode.data.repositories.AuthRepository
import es.sebas1705.axiomnode.data.repositories.GamesRepository
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val dataModule = module {
    // AppConfig singleton
    single<AppConfig> { createAppConfig() }

    // HTTP client for public mobile edge endpoints.
    single<HttpClient> {
        val config = get<AppConfig>()
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = if (config.environment.isDev) LogLevel.ALL else LogLevel.INFO
            }
        }
    }

    // Database (platform-specific initialization via platformModule)
    // Note: AxiomNodeDatabase is provided by platformModule, not here.
    // This avoids GlobalContext issues with KoinApplication composable.

    // DAOs (resolved from the database provided by platformModule)
    single { get<AxiomNodeDatabase>().gameDao() }
    single { get<AxiomNodeDatabase>().gameResultDao() }

    // Network clients – receive AppConfig for base URLs
    single { AuthHttpClient(get(), get<AppConfig>().apiBaseUrl) }
    single { GamesHttpClient(get(), get<AppConfig>().apiBaseUrl) }

    // Sync engine
    single { GameResultSyncEngine(get(), get<GamesHttpClient>()) }

    // Repositories / Use Cases
    single<AuthUseCase> {
        AuthRepository(
            httpClient = get<AuthHttpClient>(),
            config = get<AppConfig>(),
        )
    }
    single<GamesUseCase> {
        GamesRepository(
            httpClient = get<GamesHttpClient>(),
            gameDao = get(),
            gameResultDao = get(),
            syncEngine = get<GameResultSyncEngine>(),
            config = get<AppConfig>(),
        )
    }
}
