package no.nav.historisk.innsyn.service.uttrekkshentere

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.model.KolonneSchema
import no.nav.historisk.innsyn.model.Schema
import no.nav.historisk.innsyn.model.SqlType
import no.nav.historisk.innsyn.model.value.DatabaseIterator
import no.nav.historisk.innsyn.model.value.Kolonnenavn
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.repository.SchemaRepository
import no.nav.historisk.innsyn.testutil.annotation.ServiceTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@Disabled // Ikke helt klar enda
@ServiceTest
@ActiveProfiles("test")
class IdUttrekkshenterTest {

    @Autowired
    private lateinit var uttrekkshenter: IdUttrekkshenter

    @Autowired
    private lateinit var schemaRepository: SchemaRepository

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate


    val tabellnavn = Tabellnavn("TEST_OPPDATERT")

    @Test
    fun hentUttrekk() {
        val inserts = listOf(
            "insert into TEST_OPPDATERT (PS01_PERSONKEY) values (123)",
            "insert into TEST_OPPDATERT (PS01_PERSONKEY) values (456)",
            "insert into TEST_OPPDATERT (PS01_PERSONKEY) values (789)"
        )
        inserts.forEach {
            jdbcTemplate.jdbcTemplate.execute(it)
        }

        val schema = schemaRepository.finnSchema(tabellnavn) !!

        val resultat = uttrekkshenter.hentUttrekk(schema, DatabaseIterator.NullIterator(tabellnavn), 2)
        assertThat(resultat.innhold.map { listOf(it[0]) }).isEqualTo(listOf(
            listOf("123"),
            listOf("456")
        ))

        val nesteResultat = uttrekkshenter.hentUttrekk(schema, DatabaseIterator.parse(resultat.iterator), 2)
        assertThat(nesteResultat.innhold.map { listOf(it[0]) }).isEqualTo(listOf(
            listOf("789")
        ))

        val sisteResultat = uttrekkshenter.hentUttrekk(schema, DatabaseIterator.parse(nesteResultat.iterator), 2)
        assertThat(sisteResultat.innhold.map { listOf(it[0]) }).isEmpty()
    }

    @Test
    fun flereRader() {
        val schema = schemaRepository.finnSchema(tabellnavn) !!
        val rader = lagRader(30, 10)
        insertRader(rader)

        val resultat: MutableList<TestRad> = mutableListOf()
        var iterator: DatabaseIterator = DatabaseIterator.NullIterator(tabellnavn)
        var siste: List<TestRad> = emptyList()
        var startet = false
        var n = 0
        while (!startet || !siste.isEmpty()) {
            startet = true
            val res = uttrekkshenter.hentUttrekk(schema, iterator, 5)
            iterator = DatabaseIterator.parse(res.iterator)
            siste = parseRader(schema, res.innhold)
            resultat.addAll(siste)
            n++
            assertThat(n).isLessThan(1000)
        }
        assertThat(resultat).containsExactlyInAnyOrderElementsOf(rader)
    }

    @Test
    fun `kast exception dersom det har blitt kjørt baseline`() {
        val schema = schemaRepository.finnSchema(tabellnavn) !!
        insertRader(lagRader(30, 10, tidspunktForBaseline = LocalDateTime.parse("2010-01-01T00:00:00")))

        // Første uttrekk, OK
        var iterator: DatabaseIterator = DatabaseIterator.NullIterator(tabellnavn)
        val res = uttrekkshenter.hentUttrekk(schema, iterator, 5)
        iterator = DatabaseIterator.parse(res.iterator)

        // Ny baseline
        slettAlleRader()
        insertRader(lagRader(30, 10, tidspunktForBaseline = LocalDateTime.parse("2020-01-01T00:00:00")))

        val e = assertThrows<SoekeException> {
            uttrekkshenter.hentUttrekk(schema, iterator, 5)
        }
        assertThat(e.status).isEqualTo(HttpStatus.CONFLICT)
        assertThat(e.feilkode).isEqualTo(Feilkode.NY_BASELINE)
    }

    @Test
    fun erKompatibelMed() {
        assertThat(uttrekkshenter.erKompatibelMed(schema(id = true, oppdatert = true))).isTrue()

        assertThat(uttrekkshenter.erKompatibelMed(schema(id = false, oppdatert = true))).isFalse()
        assertThat(uttrekkshenter.erKompatibelMed(schema(id = true, oppdatert = false))).isFalse()
        assertThat(uttrekkshenter.erKompatibelMed(schema(id = false, oppdatert = false)))
            .isFalse()
    }

    private fun insertRader(rader: List<TestRad>) {
        val params: Array<Map<String, Any>> = rader.map {
            mapOf(
                "personKey" to it.personKey,
                "id" to it.id,
                "oppdatert" to it.oppdatert
            )
        }.toTypedArray()

        jdbcTemplate.batchUpdate("insert into $tabellnavn (PS01_PERSONKEY, ID_PS_TJEPEN, OPPDATERT) values (:personKey, :id, :oppdatert)", params)
    }

    private fun slettAlleRader() {
        jdbcTemplate.update("delete from $tabellnavn where 1=1", mutableMapOf<String, String>())
    }

    private fun parseRader(schema: Schema, rader: List<List<String?>>): List<TestRad> {
        val idIdx = schema.kolonne("ID_PS_TJEPEN").index
        val oppdatertIdx = schema.kolonne("OPPDATERT").index
        val personKeyIdx = schema.kolonne("PS01_PERSONKEY").index
        return rader.map {

            TestRad(
                id = it[idIdx]!!.toLong(),
                oppdatert = LocalDateTime.parse(it[oppdatertIdx]),
                personKey = it[personKeyIdx]!!.toLong()
            )
        }
    }

    private fun lagRader(
        antall: Int,
        batchSize: Int,
        tidspunktForBaseline: LocalDateTime = LocalDateTime.parse("2000-01-01T00:00:00")
    ): List<TestRad> {
        val foerstePersonKey: Long = 1000000
        var timestamp = tidspunktForBaseline
        val timestamps = (0 until antall).map {
            if (it % batchSize == 0) {
                timestamp = timestamp.plusMinutes(1)
            }
            timestamp
        }.toMutableList()

        timestamps.shuffle()

        return timestamps.mapIndexed { i, ts ->
            TestRad(
                id = i.toLong(),
                oppdatert = ts,
                personKey = foerstePersonKey + i
            )
        }.sortedWith(compareBy<TestRad> { it.oppdatert }.thenBy { it.id })
    }

    private fun schema(id: Boolean, oppdatert: Boolean): Schema {
        val kolonner = mutableListOf<KolonneSchema>()
        var index = 0
        kolonner.add(KolonneSchema(
            navn = Kolonnenavn("kolonne"),
            type = SqlType.STRING,
            primaryKey = false,
            index = index++
        ))

        if (id) {
            kolonner.add(KolonneSchema(
                navn = Kolonnenavn("ID_NOE"),
                type = SqlType.NUMBER,
                primaryKey = true,
                index = index++
            ))
        }
        if (oppdatert) {
            kolonner.add(KolonneSchema(
                navn = Kolonnenavn("OPPDATERT"),
                type = SqlType.NUMBER,
                primaryKey = false,
                index = index++
            ))
        }

        return Schema(
            tabellnavn = Tabellnavn("tabellnavn"),
            kolonner = kolonner
        )
    }
}

