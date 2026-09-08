package no.nav.historisk.innsyn.exception

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.net.URI

class KildesystemException(
    val kildesystem: Kildesystem,
    reason: String,
    val status: HttpStatus? = null,
    val remoteUrl: URI? = null,
    val remoteMethod: HttpMethod? = null,
    cause: Throwable? = null,
    private val additionalResponseHeaders: Map<String, List<String>> = mapOf()
) : ResponseStatusException(status ?: HttpStatus.INTERNAL_SERVER_ERROR, genererMelding(kildesystem, reason, remoteMethod, remoteUrl), cause) {
    override fun getHeaders(): HttpHeaders {
        val headers = additionalResponseHeaders
        if (headers.isEmpty()) {
            return HttpHeaders.EMPTY
        }
        val result = HttpHeaders()
        headers.forEach { (headerName: String, headerValues: List<String>) ->
            result.addAll(
                headerName, headerValues
            )
        }
        return result
    }
}

private fun genererMelding(
    kildesystem: Kildesystem,
    reason: String,
    remoteMethod: HttpMethod?,
    remoteUrl: URI?
): String {
    return "[$kildesystem] $reason ${remoteMethod ?: ""} ${remoteUrl?.scheme ?: ""}://${remoteUrl?.host ?: ""}:${remoteUrl?.port ?: ""}${remoteUrl?.path ?: ""}"
}

enum class Kildesystem {
    SELF,
    INFOTRYGD,
    OAUTH2_PROVIDER,
    STS
}