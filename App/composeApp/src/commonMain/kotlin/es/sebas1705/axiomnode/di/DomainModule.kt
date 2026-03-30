package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.domain.usecases.GetCountriesUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::GetCountriesUseCase)
}
