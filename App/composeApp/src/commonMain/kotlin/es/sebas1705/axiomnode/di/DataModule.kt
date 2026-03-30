package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.CountriesDatabase
import es.sebas1705.axiomnode.data.datasources.CountryDatasource
import es.sebas1705.axiomnode.data.datasources.DriverFactory
import es.sebas1705.axiomnode.data.irepositories.ICountryRepository
import es.sebas1705.axiomnode.data.remote.CountriesApi
import es.sebas1705.axiomnode.data.repositories.CountryRepository
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single { CountryDatasource(CountriesDatabase.invoke(get<DriverFactory>().createDriver())) }.withOptions { createdAtStart() }
    single { CountriesApi(get()) }
    singleOf(::CountryRepository).bind<ICountryRepository>()
}
