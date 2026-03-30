package es.sebas1705.axiomnode.data.irepositories

import es.sebas1705.axiomnode.data.CountryEntity

interface ICountryRepository {

    fun getCountries(): Map<String, CountryEntity>

    fun getCountry(code: String): CountryEntity?

    fun upsertCountry(code: String, country: CountryEntity)

    fun deleteCountry(code: String)
}