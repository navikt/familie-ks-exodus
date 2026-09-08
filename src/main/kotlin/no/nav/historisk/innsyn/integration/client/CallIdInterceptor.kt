package no.nav.historisk.innsyn.integration.client

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.util.*

@Component
class CallIdInterceptor(@Value("\${spring.application.name}") private val applicationName: String) : ClientHttpRequestInterceptor {

    private val consumerIdHeader = "Nav-Consumer-Id"
    private val callIdHeader = "Nav-CallId"

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val callId: String = MDC.get(callIdHeader) ?: UUID.randomUUID().toString()
        request.headers[consumerIdHeader] = applicationName
        request.headers[callIdHeader] = callId
        request.headers["callId"] = callId
        request.headers["Nav-Call-Id"] = callId
        request.headers["X-Correlation-Id"] = callId
        return execution.execute(request, body)
    }
}