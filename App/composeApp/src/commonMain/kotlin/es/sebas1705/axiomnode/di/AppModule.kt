package es.sebas1705.axiomnode.di

import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("App title")) { "Axiom Node" }
}
