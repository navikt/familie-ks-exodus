package no.nav.historisk.innsyn.integration.client

import no.nav.historisk.innsyn.exception.KildesystemException
import no.nav.historisk.innsyn.exception.Kildesystem
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class ExceptionHandlerInterceptor(private val kildesystem: Kildesystem) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        try {
            return execution.execute(request, body)
        } catch (e: Exception) {
            throw KildesystemException(
                kildesystem = kildesystem,
                reason = "Feil ved REST-kall",
                remoteUrl = request.uri,
                remoteMethod = request.method,
                cause = e
            )
        }
    }
}