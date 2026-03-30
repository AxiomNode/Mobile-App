package es.sebas1705.axiomnode.data.datasources

import es.sebas1705.axiomnode.CountriesDatabase
import es.sebas1705.axiomnode.data.CountryEntity

class CountryDatasource(
    appDatabase: CountriesDatabase
) {

    private val queries = appDatabase.countriesDatabaseQueries

    fun select(
        code: String? = null
    ): Map<String, CountryEntity> = code?.let {
        queries.selectById(code).executeAsOneOrNull()
            ?.let { mapOf(it.id to it) }
    } ?: run { queries.selectAll().executeAsList().associateBy { it.id } }

    fun count(): Long = queries.countCountries().executeAsOne()

    fun getLastCountriesSyncEpochMs(): Long? =
        queries.selectCacheMetadata().executeAsOneOrNull()?.lastCountriesSyncEpochMs

    fun setLastCountriesSyncEpochMs(value: Long) {
        queries.upsertCacheMetadata(value)
    }

    fun upsert(
        code: String,
        country: CountryEntity
    ) = queries.insertCountry(
        id = code,
        name = country.name,
        capital = country.capital,
        continent = country.continent,
        flagUrl = country.flagUrl,
        backUrl = country.backUrl
    )

    fun upsertAll(countries: List<Pair<String, CountryEntity>>) {
        queries.transaction {
            countries.forEach { (code, entity) ->
                upsert(code, entity)
            }
        }
    }

    fun clearAllCountries() {
        queries.clearCountries()
    }

    fun delete(
        code: String
    ) = queries.deleteById(code)
}