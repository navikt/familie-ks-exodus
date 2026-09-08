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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import kotlin.random.Random

@ServiceTest
@ActiveProfiles("test")
class IdOppdatertUttrekkshenterTest {

    @Autowired
    private lateinit var uttrekkshenter: IdOppdatertUttrekkshenter

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
    fun `ingen rader`() {
        val schema = schemaRepository.finnSchema(tabellnavn) !!
        val iterator: DatabaseIterator = DatabaseIterator.NullIterator(tabellnavn)
        val res = uttrekkshenter.hentUttrekk(schema, iterator, 5)
        assertThat(res.innhold).isEmpty()
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

    @Test
    fun `flereRader med sammensatt PK`() {
        val tabellnavn = Tabellnavn("test_sammensatt_pk")
        val schema = schemaRepository.finnSchema(tabellnavn) !!
        val rader = lagRaderSammensattId()
        insertRaderSammensattId(rader)

        val resultat: MutableList<TestRadSammensattPk> = mutableListOf()
        var iterator: DatabaseIterator = DatabaseIterator.NullIterator(tabellnavn)
        var siste: List<TestRadSammensattPk> = emptyList()
        var startet = false
        var n = 0
        while (!startet || !siste.isEmpty()) {
            startet = true
            val res = uttrekkshenter.hentUttrekk(schema, iterator, 5)
            iterator = DatabaseIterator.parse(res.iterator)
            siste = parseRaderSammensattId(schema, res.innhold)
            resultat.addAll(siste)
            n++
            assertThat(n).isLessThan(1000)
        }
        assertThat(resultat).containsExactlyInAnyOrderElementsOf(rader)
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
        }.shuffled()
    }

    private fun lagRaderSammensattId(): List<TestRadSammensattPk> {
        val res: MutableList<TestRadSammensattPk> = mutableListOf()

        val antallPerId = 7L

        val baseline = LocalDateTime.parse("2000-01-01T00:00:00")
        val timestamps = (0 until 10L).map {
            baseline.plusSeconds(it)
        }

        val rnd = Random(0L)

        var x = 0L
        for (i in 1..antallPerId) {
            for (j in 1..antallPerId) {
                for (k in 1..antallPerId) {
                    res.add(TestRadSammensattPk(
                        value = x++,
                        id1 = i,
                        id2 = j,
                        id3 = k,
                        oppdatert = timestamps.random(rnd)
                    ))
                }
            }
        }

        return res
    }

    private fun insertRaderSammensattId(rader: List<TestRadSammensattPk>) {
        val params: Array<Map<String, Any>> = rader.map {
            mapOf(
                "value" to it.value,
                "id1" to it.id1,
                "id2" to it.id2,
                "id3" to it.id3,
                "oppdatert" to it.oppdatert
            )
        }.toTypedArray()

        jdbcTemplate.batchUpdate("insert into test_sammensatt_pk (value, id_1, id_2, id_3, OPPDATERT) values (:value, :id1, :id2, :id3, :oppdatert)", params)
    }

    private fun parseRaderSammensattId(schema: Schema, rader: List<List<String?>>): List<TestRadSammensattPk> {
        return rader.map { kolonner ->
            TestRadSammensattPk(
                value = kolonner[schema.kolonne("VALUE").index]!!.toLong(),
                id1 = kolonner[schema.kolonne("ID_1").index]!!.toLong(),
                id2 = kolonner[schema.kolonne("ID_2").index]!!.toLong(),
                id3 = kolonner[schema.kolonne("ID_3").index]!!.toLong(),
                oppdatert = LocalDateTime.parse(kolonner[schema.kolonne("OPPDATERT").index]!!),
            )
        }
    }

    private fun schema(id: Boolean, oppdatert: Boolean): Schema {
        var index = 0

        val kolonner = mutableListOf<KolonneSchema>()
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
