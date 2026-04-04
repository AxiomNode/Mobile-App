package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import es.sebas1705.axiomnode.data.network.AuthHttpClient
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
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    // HTTP Client singleton
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }

    // Database (platform-specific initialization)
    single<AxiomNodeDatabase> { provideDatabase() }

    // Network clients
    single { AuthHttpClient(get()) }
    single { GamesHttpClient(get()) }

    // Repositories / Use Cases
    singleOf(::AuthRepository).bind<AuthUseCase>()
    singleOf(::GamesRepository).bind<GamesUseCase>()
}

// Platform-specific factory (implemented per platform)
expect fun provideDatabase(): AxiomNodeDatabase

