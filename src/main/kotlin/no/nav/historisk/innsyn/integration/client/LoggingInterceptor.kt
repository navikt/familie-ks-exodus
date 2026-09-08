package no.nav.historisk.innsyn.integration.client

import no.nav.historisk.innsyn.exception.Kildesystem
import no.nav.historisk.innsyn.utils.timeMillis
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class LoggingInterceptor(private val kildesystem: Kildesystem) : ClientHttpRequestInterceptor {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val (res, millis) = timeMillis {
            execution.execute(request, body)
        }

        logger.info("HTTP client call (${kildesystem}) [${millis}ms]: ${res.statusCode.value()} ${request.method} ${request.uri}")

        return res
    }
}