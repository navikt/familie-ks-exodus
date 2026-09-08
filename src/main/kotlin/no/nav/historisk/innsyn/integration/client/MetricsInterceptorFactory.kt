package no.nav.historisk.innsyn.integration.client

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import no.nav.historisk.innsyn.exception.Kildesystem
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class MetricsInterceptorFactory(
    private val registry: MeterRegistry,
    @Value("\${spring.application.name}") private val applicationName: String
) {
    fun forKildesystem(kildesystem: Kildesystem): ClientHttpRequestInterceptor {
        return MetricsInterceptor(kildesystem)
    }

    private inner class MetricsInterceptor(private val kildesystem: Kildesystem) : ClientHttpRequestInterceptor {
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            val uri = request.uri
            val timer =  Timer
                .builder("${applicationName}_client")
                .tags(listOf(
                    Tag.of("kildesystem", kildesystem.name),
                    Tag.of("host", uri.host),
                    Tag.of("path", uri.path)))
                .publishPercentiles(0.5, 0.95)
                .publishPercentileHistogram()
                .register(registry);
            return timer.recordCallable { execution.execute(request, body) } !!
        }
    }
}