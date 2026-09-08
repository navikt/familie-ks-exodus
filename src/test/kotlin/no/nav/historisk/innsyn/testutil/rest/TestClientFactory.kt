package no.nav.historisk.innsyn.testutil.rest

import com.nimbusds.jose.JOSEObjectType
import no.nav.historisk.innsyn.exception.Feilmelding
import no.nav.historisk.innsyn.exception.Kildesystem
import no.nav.historisk.innsyn.integration.client.ExceptionHandlerInterceptor
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.slf4j.LoggerFactory
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.util.*

class TestClientException(
    val status: HttpStatus,
    val remoteUrl: URI,
    val remoteMethod: HttpMethod,
    val feilmelding: Feilmelding?,
    message: String
) : RuntimeException(message)

@Profile("test")
@Component
class TestClientFactory(
    private val restTemplateBuilder: RestTemplateBuilder,
    private val server: MockOAuth2Server,
    private val jsonMapper: JsonMapper
) {

    fun get(port: Int, inkluderAccessToken: Boolean = true, clientId: String = "default", roller: List<String> = emptyList()): TestClient {
        val grupper = mutableListOf<String>()

        return TestClient(restTemplate(
            port = port,
            grupper = grupper,
            roller = roller,
            clientId = clientId,
            inkluderAccessToken = inkluderAccessToken
        ))
    }

    private fun restTemplate(port: Int, grupper: List<String>, roller: List<String>, clientId: String, inkluderAccessToken: Boolean): RestTemplate {

        val kildesystem = Kildesystem.SELF

        val interceptors: MutableList<ClientHttpRequestInterceptor> = mutableListOf()
        if (inkluderAccessToken) {
            interceptors.add(MockOAuth2ServerAccessTokenInterceptor(
                grupper = grupper,
                roller = roller,
                clientId = clientId
            ))
        }
        interceptors.add(ExceptionHandlerInterceptor(kildesystem))

        return restTemplateBuilder
            .errorHandler(TestErrorHandler())
            .baseUri("http://localhost:$port")
            .interceptors(interceptors)
            .build()
    }

    private inner class TestErrorHandler(): ResponseErrorHandler {
        private val logger = LoggerFactory.getLogger(javaClass)

        override fun hasError(response: ClientHttpResponse): Boolean {
            val rawStatusCode = response.statusCode.value()
            val series = HttpStatus.Series.resolve(rawStatusCode)
            return series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR
        }

        override fun handleError(url: URI, method: HttpMethod, response: ClientHttpResponse) {
            val status = response.statusCode
            val contentType = response.headers.contentType

            val body = response.body.bufferedReader().use { it.readText() }
            val feilmelding: Feilmelding? = if (contentType?.isCompatibleWith(MediaType.APPLICATION_JSON) == true) {
                try {
                    jsonMapper.readValue(body, Feilmelding::class.java)
                } catch (e: Exception) {
                    logger.warn("Kunne ikke mappe JSON-respons til Feilmelding", e)
                    null
                }
            } else {
                null
            }

            throw TestClientException(
                status = HttpStatus.valueOf(status.value()),
                feilmelding = feilmelding,
                remoteUrl = url,
                remoteMethod = method,
                message = "Feil ved REST-kall: $status $method - $url\n$body"
            )
        }
    }

    private inner class MockOAuth2ServerAccessTokenInterceptor(
        private val grupper: List<String>,
        private val roller: List<String>,
        private val clientId: String
    ) : ClientHttpRequestInterceptor {
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            val token = server.issueToken(
                issuerId = "issuer1",
                clientId = clientId,
                tokenCallback = DefaultOAuth2TokenCallback(
                    issuerId = "issuer1",
                    subject = UUID.randomUUID().toString(),
                    typeHeader = JOSEObjectType.JWT.type,
                    audience = listOf("aud-localhost"),
                    claims = mapOf("groups" to grupper, "roles" to roller),
                    expiry = 3600
                )
            )
            request.headers.setBearerAuth(token.serialize())
            return execution.execute(request, body)
        }
    }
}