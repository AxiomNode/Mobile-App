package es.sebas1705.axiomnode.presentation.country

import es.sebas1705.axiomnode.domain.models.Country

data class CountryState(
    val countries: List<Country> = emptyList(),
    val nCountries: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)