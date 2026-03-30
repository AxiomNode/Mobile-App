package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.core.currentTimeMillis
import es.sebas1705.axiomnode.core.logE
import es.sebas1705.axiomnode.core.logI
import es.sebas1705.axiomnode.data.CountryEntity
import es.sebas1705.axiomnode.data.datasources.CountryDatasource
import es.sebas1705.axiomnode.data.irepositories.ICountryRepository
import es.sebas1705.axiomnode.data.remote.CountriesApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class CountryRepository(
    private val datasource: CountryDatasource,
    private val api: CountriesApi,
) : ICountryRepository {

    override fun getCountries() = datasource.select()

    override fun getCountry(code: String) =
        datasource.select(code = code).values.firstOrNull()

    override fun upsertCountry(code: String, country: CountryEntity) {
        datasource.upsert(code, country)
    }

    override fun deleteCountry(code: String) {
        datasource.delete(code)
    }

    private fun nowEpochMs(): Long = currentTimeMillis()

    /**
     * Sincroniza desde red sólo si hace falta.
     * - Si la BD está vacía => sync
     * - Si lastSync es null => sync
     * - Si lastSync es viejo => sync
     * - Si está fresco => no hace nada
     */
    suspend fun syncIfNeeded(maxAge: Duration = 24.hours): Boolean {
        val count = datasource.count()
        val lastSync = datasource.getLastCountriesSyncEpochMs()
        val stale = lastSync == null || (nowEpochMs() - lastSync) > maxAge.inWholeMilliseconds

        if (count > 0 && !stale) {
            this.logI("CountryRepository: cache hit (count=$count, lastSync=$lastSync)")
            return false
        }

        val synced = syncFromNetworkInternal()
        if (synced > 0) datasource.setLastCountriesSyncEpochMs(nowEpochMs())
        return synced > 0
    }

    private fun sanitizeBackUrl(url: String?): String? {
        val u = url?.trim().orEmpty()
        if (u.isBlank()) return null
        // Bloqueamos fuentes que sabemos que devuelven HTML/errores
        if (u.contains("source.unsplash.com", ignoreCase = true)) return null
        if (u.contains("herokucdn.com/error-pages", ignoreCase = true)) return null
        return u
    }

    private fun toEntity(dto: es.sebas1705.axiomnode.data.remote.CountryDto): CountryEntity {
        return CountryEntity(
            id = dto.code,
            name = dto.name,
            capital = dto.capital,
            continent = dto.continent,
            flagUrl = dto.flagUrl,
            backUrl = sanitizeBackUrl(dto.backUrl),
        )
    }

    /**
     * Descarga países desde red y los guarda en la BD local.
     * IMPORTANT: nunca debe tumbar la app.
     */
    private suspend fun syncFromNetworkInternal(): Int {
        return try {
            val remote = api.fetchAllCountries()
            remote.forEach { dto ->
                datasource.upsert(dto.code, toEntity(dto))
            }
            this.logI("CountryRepository.sync: upserted=${remote.size}")
            remote.size
        } catch (t: Throwable) {
            this.logE("CountryRepository.sync failed: ${t.message}")
            0
        }
    }

    /**
     * Fuerza refresh: trae datos de red y reemplaza la cache local sólo si la red devuelve datos.
     * Si la red falla, NO borra la cache existente.
     */
    suspend fun forceRefresh(): Boolean {
        return try {
            val remote = api.fetchAllCountries()
            if (remote.isEmpty()) {
                this.logE("CountryRepository.forceRefresh: remote vacío")
                return false
            }

            val entities = remote.map { dto ->
                dto.code to toEntity(dto)
            }

            // Reemplazo atómico
            datasource.clearAllCountries()
            datasource.upsertAll(entities)
            datasource.setLastCountriesSyncEpochMs(nowEpochMs())

            this.logI("CountryRepository.forceRefresh: replaced=${entities.size}")
            true
        } catch (t: Throwable) {
            this.logE("CountryRepository.forceRefresh failed: ${t.message}")
            throw t
        }
    }

    /**
     * Lazy: si un país no tiene backUrl, intenta resolverlo y lo persiste.
     * Devuelve el backUrl guardado o null si no se pudo.
     */
    suspend fun ensureBackUrl(code: String): String? {
        val current = getCountry(code) ?: return null
        val existing = sanitizeBackUrl(current.backUrl)
        if (!existing.isNullOrBlank()) return existing

        val fetched = sanitizeBackUrl(api.fetchBackUrl(current.name, current.capital))
        if (fetched.isNullOrBlank()) return null

        val updated = current.copy(backUrl = fetched)
        datasource.upsert(code, updated)
        return fetched
    }
}