package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.data.datasources.AndroidDriverFactory
import es.sebas1705.axiomnode.data.datasources.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<DriverFactory> { AndroidDriverFactory(get()) }
    }