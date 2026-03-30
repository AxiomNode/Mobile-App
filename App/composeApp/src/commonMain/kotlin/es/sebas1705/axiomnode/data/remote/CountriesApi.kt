package es.sebas1705.axiomnode.data.remote

import es.sebas1705.axiomnode.core.logI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API simple usando https://restcountries.com + imagen representativa desde MediaWiki.
 */
class CountriesApi(
    private val httpClient: HttpClient,
) {

    suspend fun fetchAllCountries(): List<CountryDto> {
        val response = httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "restcountries.com"
                appendPathSegments("v3.1", "all")
                parameters.append("fields", "cca2,name,capital,continents,flags")
            }
        }

        val body: List<RestCountryResponse> = response.body()
        this.logI("CountriesApi: fetched=${body.size}")

        val base = body.mapNotNull { it.toDtoOrNull() }

        // backUrl significativo (país primero, capital fallback).
        // Nota: hace llamadas extra; como se cachea en BD + TTL/refresh, es aceptable.
        return base.map { dto ->
            val back = fetchBackUrlForTitle(dto.name)
                ?: fetchBackUrlForTitle(dto.capital)
            dto.copy(backUrl = back)
        }
    }

    /**
     * Lazy: obtener un backUrl para un país concreto.
     * Prioridad: nombre del país -> capital.
     */
    suspend fun fetchBackUrl(countryName: String, capital: String): String? {
        return fetchBackUrlForTitle(countryName)
            ?: fetchBackUrlForTitle(capital)
    }

    private suspend fun fetchBackUrlForTitle(title: String): String? {
        val safeTitle = title.trim().takeIf { it.isNotEmpty() } ?: return null
        val resp = fetchPageImages(safeTitle) ?: return null

        // 1) Preferimos original (URL directa)
        resp.original?.source
            ?.takeIf { isUsableImageUrl(it) }
            ?.let { return it }

        // 2) si no, thumbnail
        resp.thumbnail?.source
            ?.takeIf { isUsableImageUrl(it) }
            ?.let { return it }

        return null
    }

    private fun isUsableImageUrl(url: String): Boolean {
        val u = url.lowercase()
        if (!(u.startsWith("http://") || u.startsWith("https://"))) return false
        // Kamel/engine puede fallar con webp en algunas plataformas
        if (u.endsWith(".webp")) return false
        // Aceptamos formatos comunes
        if (u.endsWith(".jpg") || u.endsWith(".jpeg") || u.endsWith(".png") || u.endsWith(".gif")) return true
        // Si no tiene extensión (a veces Wikimedia no la pone al final), lo aceptamos si viene de upload.wikimedia.org
        return u.contains("upload.wikimedia.org")
    }

    private suspend fun fetchPageImages(title: String): MediaWikiPageImages? {
        return try {
            val resp: MediaWikiQueryResponse = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "en.wikipedia.org"
                    appendPathSegments("w", "api.php")
                    parameters.append("format", "json")
                    parameters.append("action", "query")
                    parameters.append("redirects", "1")
                    parameters.append("prop", "pageimages")
                    parameters.append("piprop", "thumbnail|original")
                    parameters.append("pithumbsize", "1280")
                    parameters.append("titles", title)
                }
            }.body()

            resp.query?.pages?.values?.firstOrNull()?.let { page ->
                MediaWikiPageImages(
                    thumbnail = page.thumbnail,
                    original = page.original
                )
            }
        } catch (_: Throwable) {
            null
        }
    }
}

private data class MediaWikiPageImages(
    val thumbnail: MediaWikiQueryResponse.Page.Image?,
    val original: MediaWikiQueryResponse.Page.Image?,
)

@Serializable
private data class MediaWikiQueryResponse(
    @SerialName("query") val query: Query? = null,
) {
    @Serializable
    data class Query(
        @SerialName("pages") val pages: Map<String, Page> = emptyMap(),
    )

    @Serializable
    data class Page(
        @SerialName("thumbnail") val thumbnail: Image? = null,
        @SerialName("original") val original: Image? = null,
    ) {
        @Serializable
        data class Image(
            @SerialName("source") val source: String? = null,
            @SerialName("width") val width: Int? = null,
            @SerialName("height") val height: Int? = null,
        )
    }
}

@Serializable
private data class RestCountryResponse(
    @SerialName("cca2") val code: String? = null,
    @SerialName("name") val name: Name? = null,
    @SerialName("capital") val capital: List<String> = emptyList(),
    @SerialName("continents") val continents: List<String> = emptyList(),
    @SerialName("flags") val flags: Flags? = null,
) {
    @Serializable
    data class Name(
        @SerialName("common") val common: String? = null,
    )

    @Serializable
    data class Flags(
        @SerialName("png") val png: String? = null,
        @SerialName("svg") val svg: String? = null,
    )

    fun toDtoOrNull(): CountryDto? {
        val safeCode = code?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return CountryDto(
            code = safeCode,
            name = name?.common.orEmpty(),
            capital = capital.firstOrNull().orEmpty(),
            continent = continents.firstOrNull().orEmpty(),
            flagUrl = flags?.png ?: flags?.svg,
            backUrl = null,
        )
    }
}

/** DTO neutral en commonMain */
@Serializable
data class CountryDto(
    val code: String,
    val name: String,
    val capital: String,
    val continent: String,
    val flagUrl: String? = null,
    val backUrl: String? = null,
)
