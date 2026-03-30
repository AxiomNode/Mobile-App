package es.sebas1705.axiomnode.domain.usecases

import es.sebas1705.axiomnode.core.logE
import es.sebas1705.axiomnode.data.irepositories.ICountryRepository
import es.sebas1705.axiomnode.data.repositories.CountryRepository
import es.sebas1705.axiomnode.domain.models.Country

/**
 * Use case: usa cache local (SQLDelight) y sólo sincroniza desde red si hace falta.
 */
class GetCountriesUseCase(
    private val repository: ICountryRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): List<Country> {
        (repository as? CountryRepository)?.let { repo ->
            if (forceRefresh) {
                // si falla, queremos que la UI lo sepa
                repo.forceRefresh()
            } else {
                try {
                    repo.syncIfNeeded()
                } catch (t: Throwable) {
                    this.logE("GetCountriesUseCase sync failed: ${t.message}")
                }
            }
        }

        return repository.getCountries().map { Country(it.key, it.value) }
    }
}