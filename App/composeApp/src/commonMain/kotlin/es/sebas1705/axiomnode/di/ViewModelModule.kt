package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.presentation.country.CountryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::CountryViewModel)
}
