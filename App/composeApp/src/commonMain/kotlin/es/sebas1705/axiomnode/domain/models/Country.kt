package es.sebas1705.axiomnode.domain.models

import es.sebas1705.axiomnode.data.CountryEntity

data class Country(
    val code: String,
    val name: String,
    val capital: String,
    val continent: String,
    val flagUrl: String?,
    val backUrl: String?
) {

    constructor(code: String, countryData: CountryEntity) : this(
        code,
        countryData.name,
        countryData.capital,
        countryData.continent,
        countryData.flagUrl,
        countryData.backUrl
    )
}