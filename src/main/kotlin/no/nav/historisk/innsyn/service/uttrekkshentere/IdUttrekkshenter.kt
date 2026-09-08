package no.nav.historisk.innsyn.service.uttrekkshentere

import no.nav.historisk.innsyn.dto.KolonnebeskrivelseDto
import no.nav.historisk.innsyn.dto.SchemaDto
import no.nav.historisk.innsyn.dto.UttrekkResponse
import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.model.Schema
import no.nav.historisk.innsyn.model.SqlType
import no.nav.historisk.innsyn.model.value.DatabaseIterator
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.service.Uttrekkshenter
import no.nav.historisk.innsyn.utils.findAtMostOne
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val OPPDATERT_KOLONNE_NAVN = "OPPDATERT"

@Component
class IdUttrekkshenter(private val jdbcTemplate: NamedParameterJdbcTemplate) : Uttrekkshenter {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun hentUttrekk(schema: Schema, iterator: DatabaseIterator, antallRader: Long): UttrekkResponse {

        val tidspunktForSisteBaseline = finnTidspunktForSisteBaseline(schema.tabellnavn)

        val rader: List<IdRad> = when (iterator) {
            is DatabaseIterator.IdIterator -> {
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

        val nesteIterator: DatabaseIterator = nesteIdIterator(iterator, tidspunktForSisteBaseline, rader)

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
    ): List<IdRad> {
        val mapper = rowMapper(schema)

        val kolonner = schema.kolonner.map { it.navn }.joinToString(separator = ", ")
        val idKolonne = schema.idKolonne()

        val sql = """
            |select $kolonner 
            |from ${schema.tabellnavn.value} 
            |ORDER BY ${idKolonne.kolonne.navn} 
            |FETCH FIRST :antallRader ROWS ONLY""".trimMargin()

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
        iterator: DatabaseIterator.IdIterator
    ): List<IdRad> {
        val mapper = rowMapper(schema)

        val kolonner = schema.kolonner.map { it.navn }.joinToString(separator = ", ")
        val idKolonne = schema.idKolonne()

        val sql = """
            |select $kolonner 
            |from ${schema.tabellnavn} 
            |    where ${idKolonne.kolonne.navn} > :sisteId
            |    ORDER BY ${idKolonne.kolonne.navn} FETCH FIRST :antallRader ROWS ONLY
        """.trimMargin()

        val params = mapOf<String, Any>(
            "sisteId" to iterator.sisteId.toLong(),
            "antallRader" to antallRader
        )

        logger.debug("finnRaderMedIterator: Kjører følgende SQL (params: $params): $sql")

        val rader = jdbcTemplate.query(sql, params, mapper)
        return rader
    }

    override fun erKompatibelMed(schema: Schema): Boolean {
        return false // todo: Denne er ikke helt klar. Vi må ordne presedens og så bør vi sørge for at den virker uten OPPDATERT-felt.

        val harIdKolonne = schema.kolonner.find { it.navn.value.startsWith("ID_") } != null
        val harOppdatertKolonne = schema.kolonner.find { it.navn.value == "OPPDATERT" } != null
        val erKompatibel = harIdKolonne && harOppdatertKolonne

        logger.debug("Uttrekkshenter kompatibel: $erKompatibel, harIdKolonne=$harIdKolonne, harOppdatertKolonne=$harOppdatertKolonne")

        return erKompatibel
    }

    private fun rowMapper(schema: Schema): RowMapper<IdRad> {
        val mapper = RowMapper<IdRad> { rs, _ ->
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
            IdRad(schema, kolonner)
        }
        return mapper
    }

    private fun finnTidspunktForSisteBaseline(tabellnavn: Tabellnavn): LocalDateTime? {
        val sql = """select min($OPPDATERT_KOLONNE_NAVN) from $tabellnavn"""
        val res: LocalDateTime? = jdbcTemplate.query(sql) { rs, _ ->
            rs.getTimestamp(1).toLocalDateTime()
        }.findAtMostOne("Forventet å finne maksimalt 1 minimumsverdi for $OPPDATERT_KOLONNE_NAVN")
        return res
    }

    override val order: Int
        get() = 5
}

fun nesteIdIterator(
    opprinneligIterator: DatabaseIterator,
    tidspunktForSisteBaseline: LocalDateTime?,
    rader: List<IdRad>
): DatabaseIterator {
    val sisteRad = rader.maxByOrNull { it.id }
        ?: return opprinneligIterator

    return DatabaseIterator.IdIterator(
        tabellnavn = opprinneligIterator.tabellnavn,
        tidspunktForSisteBaseline = tidspunktForSisteBaseline,
        sisteId = sisteRad.id.toString()
    )
}

class IdRad(schema: Schema, val rad: List<String?>) {
    val id: Long = rad[schema.idKolonne().index]?.toLong() ?: throw IllegalArgumentException("Forventet en ID-kolonne")
}
