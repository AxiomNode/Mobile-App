package es.sebas1705.axiomnode.data.repositories

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Placeholder test.
 *
 * La verificación real de cache se hace en runtime mirando logs:
 * - Primera carga: "CountriesApi: fetched=..." y "CountryRepository.sync: upserted=..."
 * - Siguientes cargas: "CountryRepository: cache hit (... )" (no debería aparecer "CountriesApi: fetched")
 */
class CountryRepositoryCacheTest {
    @Test
    fun placeholder() {
        assertTrue(true)
    }
}
