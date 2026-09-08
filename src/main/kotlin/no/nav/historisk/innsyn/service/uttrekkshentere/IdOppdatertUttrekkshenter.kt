package no.nav.historisk.innsyn.service.uttrekkshentere

import no.nav.historisk.innsyn.dto.KolonnebeskrivelseDto
import no.nav.historisk.innsyn.dto.SchemaDto
import no.nav.historisk.innsyn.dto.UttrekkResponse
import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.exception.UttrekkException
import no.nav.historisk.innsyn.model.KolonneSchema
import no.nav.historisk.innsyn.model.Schema
import no.nav.historisk.innsyn.model.SqlType
import no.nav.historisk.innsyn.model.value.DatabaseIterator
import no.nav.historisk.innsyn.model.value.Kolonnenavn
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.service.Uttrekkshenter
import no.nav.historisk.innsyn.utils.findAtMostOne
import no.nav.historisk.innsyn.utils.timeMillis
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

private val OPPDATERT_KOLONNE = Kolonnenavn("OPPDATERT")

@Component
class IdOppdatertUttrekkshenter(private val jdbcTemplate: NamedParameterJdbcTemplate) : Uttrekkshenter {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun hentUttrekk(schema: Schema, iterator: DatabaseIterator, antallRader: Long): UttrekkResponse {

        val tidspunktForSisteBaseline = finnTidspunktForSisteBaseline(schema.tabellnavn)

        val rader: List<IdOppdatertRad> = when (iterator) {
            is DatabaseIterator.IdOppdatertIterator -> {
                if (iterator.tidspunktForSisteBaseline != null) {
                    if (iterator.tidspunktForSisteBaseline != tidspunktForSisteBaseline) {
                        val detaljertBeskrivelse =
                            "En ny baseline har blitt tatt på Oracle databasen siden sist spørring. Angitt " +
                                    "iterator er derfor ikke lenger gyldig. Rett måte å håndtere denne feilen på er " +
                                    "ved å 1) slette alle data som allerede er overført (for alle tabeller), 2) starte " +
                                    "hele importjobben på nytt (for alle tabeller). " +
                                    "Det anbefales IKKE å forsøke å fortsette å bygge på de dataene som allerede er overført, " +
                                    "da det er nesten garantert at disse ikke lenger vil være konsistent med de dataene " +
                                    "som nå ligger i Oracle databasen."
                        logger.info(detaljertBeskrivelse)
                        throw SoekeException(
                            feilkode = Feilkode.NY_BASELINE,
                            beskrivelse = "Ny baseline",
                            detaljertBeskrivelse = detaljertBeskrivelse
                        )
                    }
                }
                finnRaderMedIterator(schema, antallRader, iterator)
            }
            is DatabaseIterator.NullIterator -> finnRader(schema, antallRader)
            else -> throw IllegalArgumentException("Denne iteratoren er ikke støttet: ${iterator.javaClass.simpleName}") // todo: bør ha en måte å ugyldiggjøre iteratorer på
        }

        val nesteIterator: DatabaseIterator = nesteIdOpprettetOppdatertIterator(iterator, tidspunktForSisteBaseline, rader)

        return UttrekkResponse(
            iterator = nesteIterator.toDtoString(),
            schema = SchemaDto(
                kolonner = schema.kolonner.map { KolonnebeskrivelseDto(navn = it.navn.value) }
            ),
            innhold = rader.map { it.rad }
        )
    }
    
    private fun finnRader(
        schema: Schema,
        antallRader: Long
    ): List<IdOppdatertRad> {
        val mapper = rowMapper(schema)

        val kolonner = schema.kolonner.map { it.navn }.joinToString(separator = ", ")

        val primaryKey = schema.primaryKey.map { it.navn }
        val iteratorKolonner: List<Kolonnenavn> = listOf(OPPDATERT_KOLONNE) + primaryKey
        val iteratorKolonnerSql = iteratorKolonner.joinToString(", ")

        val sql = """
            select $kolonner 
            from ${schema.tabellnavn} 
            ORDER BY $iteratorKolonnerSql
            FETCH FIRST :antallRader ROWS ONLY
        """.trimIndent()
        val params = mapOf<String, Any>(
            "antallRader" to antallRader
        )
        logger.debug("finnRader: Kjører følgende SQL: $sql")
        val rader = jdbcTemplate.query(sql, params, mapper)
        return rader
    }

    private fun finnRaderMedIterator(
        schema: Schema,
        antallRader: Long,
        iterator: DatabaseIterator.IdOppdatertIterator
    ): List<IdOppdatertRad> {
        val mapper = rowMapper(schema)

        val kolonner = schema.kolonner.map { it.navn }.joinToString(separator = ", ")

        val oppdatertKolonne = schema.kolonne(OPPDATERT_KOLONNE)
            ?: throw UttrekkException(
                feilkode = Feilkode.SERVERFEIL,
                tabellnavn = schema.tabellnavn,
                beskrivelse = "Tabell mangler OPPDATERT-kolonne"
            )

        val primaryKey = schema.primaryKey

        val iteratorKolonner: List<KolonneSchema> = listOf(oppdatertKolonne) + primaryKey

        val iteratorKolonnerSql = iteratorKolonner.joinToString(", ") { it.navn.value }
        val iteratorRest = iteratorKolonner.toMutableList()

        val res = mutableListOf<IdOppdatertRad>()

        while (iteratorRest.size >= 1 && antallRader > res.size) {
            //language=oraclesqlplus
            val sql = """
                select $kolonner from ${schema.tabellnavn}
                where ${whereKlausul(iteratorRest.map { it.navn })}
                order by $iteratorKolonnerSql
                fetch first :antallRader rows only
            """.trimIndent()

            val antallRaderParams = mapOf(
                "antallRader" to antallRader - res.size
            )

            val params = iteratorRest.map {
                val p = iterator.sisteId[it.navn]
                    ?: throw UttrekkException(
                            feilkode = Feilkode.NY_BASELINE,
                            tabellnavn = schema.tabellnavn,
                            beskrivelse = "Ugyldig iterator (Primary Key)",
                            cause = null
                        )
                it.navn.value to mapParam(it, p)
            }.toMap() + antallRaderParams

            logger.trace("finnRaderMedIterator: Kjører følgende SQL (params: {}): {}", params, sql)
            val (rader, millis) = timeMillis {
                jdbcTemplate.query(sql, params, mapper)
            }

            logger.debug("finnRaderMedIterator: Kjørte følgende SQL (tid: {}ms, params: {}): {}", millis, params, sql)

            res.addAll(rader)

            iteratorRest.removeLast()
        }

        return res
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        var i = 0
        while (i < hex.length) {
            val byte = hex.substring(i, i + 2).toInt(16).toByte()
            result[i / 2] = byte
            i += 2
        }
        return result
    }

    private fun mapParam(it: KolonneSchema, p: String): Any {
        return when (it.type) {
            SqlType.STRING -> p
            SqlType.NUMBER -> p.toLong()
            SqlType.TIMESTAMP -> LocalDateTime.parse(p)
            SqlType.DATE -> LocalDate.parse(p)
            SqlType.BINARY -> hexStringToByteArray(p)
        }
    }

    override fun erKompatibelMed(schema: Schema): Boolean {

        val harPrimaryKey = schema.primaryKeyOrNull != null
        val harOppdatertKolonne = schema.kolonner.find { it.navn == OPPDATERT_KOLONNE} != null
        val erKompatibel = harPrimaryKey && harOppdatertKolonne

        logger.debug("Uttrekkshenter kompatibel: $erKompatibel, harPrimaryKey=$harPrimaryKey, harOppdatertKolonne=$harOppdatertKolonne")

        return erKompatibel
    }

    private fun rowMapper(schema: Schema): RowMapper<IdOppdatertRad> {
        val mapper = RowMapper<IdOppdatertRad> { rs, _ ->
            val kolonner: List<String?> = schema.kolonner.mapIndexed { i, kolonne ->
                val idx = i + 1
                when (kolonne.type) {
                    SqlType.STRING -> rs.getString(idx)
                    SqlType.NUMBER -> rs.getBigDecimal(idx)?.toString()
                    SqlType.TIMESTAMP -> rs.getTimestamp(idx)?.toLocalDateTime()?.toString()
                    SqlType.DATE -> rs.getDate(idx)?.toLocalDate()?.toString()
                    SqlType.BINARY -> rs.getBytes(idx)?.let { bytes ->
                        bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) } }
                }
            }
            IdOppdatertRad(schema, kolonner)
        }
        return mapper
    }

    private fun finnTidspunktForSisteBaseline(tabellnavn: Tabellnavn): LocalDateTime? {
        val sql = """select min($OPPDATERT_KOLONNE) from $tabellnavn"""

        val res = jdbcTemplate.query(sql, emptyMap<String,String>()){ rs, _ ->
            rs.getTimestamp(1)?.toLocalDateTime()
        }.findAtMostOne("Forventet å finne maksimalt 1 minimumsverdi for $OPPDATERT_KOLONNE")

        return res
    }

    override val order: Int
        get() = 0
}

private fun nesteIdOpprettetOppdatertIterator(
    opprinneligIterator: DatabaseIterator,
    tidspunktForSisteBaseline: LocalDateTime?,
    rader: List<IdOppdatertRad>
): DatabaseIterator {
    val sisteRad = rader.lastOrNull()
        ?: return opprinneligIterator

    return DatabaseIterator.IdOppdatertIterator(
        tabellnavn = opprinneligIterator.tabellnavn,
        tidspunktForSisteBaseline = tidspunktForSisteBaseline,
        sisteId = sisteRad.idOppdatert
    )
}

private class IdOppdatertRad(val schema: Schema, val rad: List<String?>) {
    init {
        require(rad.size == schema.kolonner.size) {
            "Rad og schema har ikke samme antallet kolonner: rad=${rad.size}, schema=${schema.kolonner.size}"
        }
    }

    val idOppdatert: Map<Kolonnenavn, String>
        get() {
            val oppdatertKolonne = schema.kolonne(OPPDATERT_KOLONNE)
                ?: throw UttrekkException(
                    feilkode = Feilkode.SERVERFEIL,
                    tabellnavn = schema.tabellnavn,
                    beskrivelse = "Tabell mangler $OPPDATERT_KOLONNE."
                )
            val primaryKey = schema.primaryKey
            return primaryKey.map { it.navn to rad[it.index]!! }.toMap() + mapOf(OPPDATERT_KOLONNE to rad[oppdatertKolonne.index]!!)
        }

    val oppdatert: LocalDateTime =
        LocalDateTime.parse(rad[schema.kolonne(OPPDATERT_KOLONNE.value).index]?: throw IllegalArgumentException("Forventet en OPPDATERT-kolonne"))
}

fun whereKlausul(kolonner: List<Kolonnenavn>): String {
    require(kolonner.size > 0) { "Tom liste med iterator-kolonner" }

    val likhet = kolonner.subList(0, kolonner.size - 1)
        .map { "${it} = :${it}" }
    val siste = kolonner.last()
    val ulikhet = "${siste} > :${siste}"

    val alle: List<String> = likhet + listOf(ulikhet)
    return alle.joinToString("\n and ")
}