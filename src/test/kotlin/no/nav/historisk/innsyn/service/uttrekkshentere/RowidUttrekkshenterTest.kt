package no.nav.historisk.innsyn.service.uttrekkshentere

import no.nav.historisk.innsyn.model.value.DatabaseIterator
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.repository.SchemaRepository
import no.nav.historisk.innsyn.testutil.annotation.ServiceTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles

@ServiceTest
@ActiveProfiles("test")
internal class RowidUttrekkshenterTest {
    @Autowired
    private lateinit var rowidUttrekkshenter: RowidUttrekkshenter

    @Autowired
    private lateinit var schemaRepository: SchemaRepository

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    internal fun setUp() {
        val inserts = listOf(
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (123)",
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (456)",
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (789)"
        )
        inserts.forEach {
            jdbcTemplate.jdbcTemplate.execute(it)
        }
    }

    @Test
    fun hentUttrekk() {
        val tabellnavn = Tabellnavn("TEST_INGEN_TIMESTAMP")
        val schema = schemaRepository.finnSchema(tabellnavn) !!

        val resultat = rowidUttrekkshenter.hentUttrekk(schema, DatabaseIterator.NullIterator(tabellnavn), 2)
        assertThat(resultat.innhold).isEqualTo(listOf(
            listOf("123"),
            listOf("456")
        ))

        val nesteResultat = rowidUttrekkshenter.hentUttrekk(schema, DatabaseIterator.parse(resultat.iterator), 2)
        assertThat(nesteResultat.innhold).isEqualTo(listOf(
            listOf("789")
        ))

        val sisteResultat = rowidUttrekkshenter.hentUttrekk(schema, DatabaseIterator.parse(nesteResultat.iterator), 2)
        assertThat(sisteResultat.innhold).isEmpty()
    }

    @Test
    fun erKompatibelMed() {
        val schema = schemaRepository.finnSchema(Tabellnavn("TEST_INGEN_TIMESTAMP")) !!
        assertThat(rowidUttrekkshenter.erKompatibelMed(schema)).isTrue()
    }
}