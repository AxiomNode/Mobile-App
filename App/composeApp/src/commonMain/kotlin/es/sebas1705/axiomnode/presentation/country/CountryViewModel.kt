package es.sebas1705.axiomnode.presentation.country

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.core.logE
import es.sebas1705.axiomnode.core.logI
import es.sebas1705.axiomnode.data.repositories.CountryRepository
import es.sebas1705.axiomnode.domain.usecases.GetCountriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CountryViewModel(
    private val getCountriesUseCase: GetCountriesUseCase,
    private val countryRepository: CountryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CountryState())
    val uiState = _uiState.asStateFlow()

    init {
        getCountries()
    }

    private fun getCountries() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            try {
                val countries = getCountriesUseCase()
                logI("Countries: $countries")
                _uiState.update {
                    it.copy(
                        countries = countries,
                        nCountries = countries.size,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
            } catch (t: Throwable) {
                logE("CountryViewModel.getCountries failed: ${t.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = t.message ?: "Error cargando países"
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val countries = getCountriesUseCase(forceRefresh = true)
                _uiState.update {
                    it.copy(
                        countries = countries,
                        nCountries = countries.size,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
            } catch (t: Throwable) {
                logE("CountryViewModel.refresh failed: ${t.message}")
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = t.message ?: "Error refrescando países"
                    )
                }
            }
        }
    }

    fun ensureBackImage(countryCode: String) {
        viewModelScope.launch {
            runCatching {
                val backUrl = countryRepository.ensureBackUrl(countryCode)
                if (!backUrl.isNullOrBlank()) {
                    // refrescamos lista desde BD (ligero) para que el item tenga backUrl
                    val countries = getCountriesUseCase(forceRefresh = false)
                    _uiState.update {
                        it.copy(
                            countries = countries,
                            nCountries = countries.size,
                            error = null
                        )
                    }
                }
            }.onFailure { t ->
                logE("ensureBackImage failed: ${t.message}")
            }
        }
    }
}