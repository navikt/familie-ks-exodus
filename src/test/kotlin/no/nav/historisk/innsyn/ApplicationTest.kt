package no.nav.historisk.innsyn

import no.nav.historisk.innsyn.testutil.annotation.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@IntegrationTest
class ApplicationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var restTemplateBuilder: RestTemplateBuilder

    private val restTemplate: RestTemplate
        get() {
            return restTemplateBuilder
                .baseUri("http://localhost:$port")
                .build()
        }

    @Test
    fun contextLoads() {
    }

    @Test
    fun health() {
        restTemplate.getForObject<String>("/actuator/health")
    }
}
