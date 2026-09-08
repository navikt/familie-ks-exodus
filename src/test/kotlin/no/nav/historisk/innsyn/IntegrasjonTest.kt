package no.nav.historisk.innsyn

import no.nav.historisk.innsyn.dto.CountRequest
import no.nav.historisk.innsyn.dto.UttrekkRequest
import no.nav.historisk.innsyn.testutil.annotation.IntegrationTest
import no.nav.historisk.innsyn.testutil.rest.TestClient
import no.nav.historisk.innsyn.testutil.rest.TestClientException
import no.nav.historisk.innsyn.testutil.rest.TestClientFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

@IntegrationTest
class IntegrasjonTest {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var clientFactory: TestClientFactory

    private val clientMedTilgang: TestClient
        get() = clientFactory.get(port, roller = listOf("test_oppdatert", "test_ingen_timestamp"))

    private val clientUtenTilgang: TestClient
        get() = clientFactory.get(port, roller = emptyList())

    private val clientUtenAccessToken: TestClient
        get() = clientFactory.get(port, inkluderAccessToken = false)

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    internal fun setUp() {
        val inserts = listOf(
            "insert into TEST_OPPDATERT (PS01_PERSONKEY) values (99123123123)",
            "insert into TEST_OPPDATERT (PS01_PERSONKEY) values (23432423434)",
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (123)",
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (456)",
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (789)"
        )
        inserts.forEach {
            jdbcTemplate.jdbcTemplate.execute(it)
        }
    }

    @Test
    internal fun `henter data fra tabeller`() {
        val resultat = clientMedTilgang.hentUttrekk(UttrekkRequest(
            tabellnavn = "TEST_OPPDATERT",
            iterator = null,
            antallRader = 10
        ))
        assertThat(resultat.innhold).isNotEmpty

        val resultat2 = clientMedTilgang.hentUttrekk(UttrekkRequest(
            tabellnavn = "TEST_INGEN_TIMESTAMP",
            iterator = null,
            antallRader = 10
        ))
        assertThat(resultat2.innhold).isNotEmpty
    }

    @Test
    internal fun `skal telle riktig antall rader`() {
        assertThat(clientMedTilgang.tellRader(CountRequest(tabellnavn = "TEST_INGEN_TIMESTAMP")).antall).isEqualTo(3)
    }

    @Test
    internal fun `skal ikke få data uten å ha gyldig token`() {
        val e = assertThrows<TestClientException> {
            clientUtenTilgang.hentUttrekk(UttrekkRequest(
                tabellnavn = "TEST_OPPDATERT",
                iterator = null,
                antallRader = 10
            ))
        }
        assertThat(e.status).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    internal fun `skal ikke ha tilgang uten access token`() {
        val e = assertThrows<TestClientException> {
            clientUtenAccessToken.hentUttrekk(UttrekkRequest(
                tabellnavn = "TEST_OPPDATERT",
                iterator = null,
                antallRader = 10
            ))
        }
        assertThat(e.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}