package no.nav.historisk.innsyn.integration.client

import no.nav.historisk.innsyn.exception.KildesystemException
import no.nav.historisk.innsyn.exception.Kildesystem
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.Series
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import java.net.URI

class ClientErrorHandler(private val kildesystem: Kildesystem) : ResponseErrorHandler {
    override fun hasError(response: ClientHttpResponse): Boolean {
        val series = Series.resolve(response.statusCode.value())
        return series == Series.CLIENT_ERROR || series == Series.SERVER_ERROR
    }

    override fun handleError(url: URI, method: HttpMethod, response: ClientHttpResponse) {
        val status = response.statusCode

        val contentType = response.headers.contentType
        val content = if (contentType?.isCompatibleWith(MediaType.APPLICATION_JSON) == true || contentType?.type == "text") {
            response.body.bufferedReader().use { it.readText() }
        } else {
            "[ikke tekst-respons ($contentType)]"
        }

        throw KildesystemException(
            kildesystem = kildesystem,
            reason = "Feil ved REST-kall: $content",
            status = HttpStatus.resolve(status.value()),
            remoteUrl = url,
            remoteMethod = method,
            additionalResponseHeaders = headers(response)
        )
    }

    private fun headers(response: ClientHttpResponse): Map<String, MutableList<String>> {
        val oauthHeader = "WWW-Authenticate"
        val header = response.headers[oauthHeader]

        val headers = if (header != null) mapOf(oauthHeader to header) else emptyMap()
        return headers
    }
}