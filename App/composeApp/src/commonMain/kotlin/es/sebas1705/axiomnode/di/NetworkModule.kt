package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.network.createHttpClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

val networkModule = module {
    single<HttpClient> { createHttpClient() }
}
