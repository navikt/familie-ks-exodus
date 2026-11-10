package no.nav.historisk.innsyn

import no.nav.historisk.innsyn.testutil.annotation.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate

@IntegrationTest
class ApplicationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var restTemplateBuilder: RestTemplateBuilder

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var actuatorHandlerMapping: WebMvcEndpointHandlerMapping

    private val restTemplate: RestTemplate
        get() {
            return restTemplateBuilder
                .baseUri("http://localhost:$port")
                .build()
        }

    @Test
    fun contextLoads() {
    }

    @ParameterizedTest
    @ValueSource(strings = ["/internal", "/internal/health/liveness", "/internal/health/readiness", "/internal/prometheus"])
    fun `helseprober og prometheus er tilgjengelige uten token`(path: String) {
        val response = restTemplate.getForEntity(path, String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `actuator bruker bare internal-stier og eksponerer ikke info`() {
        val paths = actuatorHandlerMapping.handlerMethods.keys.flatMap { it.patternValues }

        assertThat(paths).isNotEmpty().allMatch { it.startsWith("/internal") }
        assertThat(paths).doesNotContain("/internal/info")
    }

    @Test
    fun `info-endepunktet er deaktivert`() {
        assertThat(applicationContext.getBeansOfType(InfoEndpoint::class.java)).isEmpty()
    }
}
