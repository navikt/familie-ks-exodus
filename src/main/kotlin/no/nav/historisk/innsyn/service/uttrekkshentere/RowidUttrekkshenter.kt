package no.nav.historisk.innsyn.service.uttrekkshentere

import no.nav.historisk.innsyn.dto.KolonnebeskrivelseDto
import no.nav.historisk.innsyn.dto.SchemaDto
import no.nav.historisk.innsyn.dto.UttrekkResponse
import no.nav.historisk.innsyn.model.Schema
import no.nav.historisk.innsyn.model.SqlType
import no.nav.historisk.innsyn.model.value.DatabaseIterator
import no.nav.historisk.innsyn.service.Uttrekkshenter
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class RowidUttrekkshenter(private val jdbcTemplate: NamedParameterJdbcTemplate) : Uttrekkshenter {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun hentUttrekk(schema: Schema, iterator: DatabaseIterator, antallRader: Long): UttrekkResponse {

        val sorterteRaderMedRowId = when(iterator) {
            is DatabaseIterator.NullIterator -> finnRader(schema, antallRader)
            is DatabaseIterator.RowidIterator -> finnRaderStoerreEnn(schema, iterator, antallRader)
            else -> throw IllegalArgumentException("Iterator ikke støttet: ${iterator.javaClass.simpleName}") // todo: bedre håndtering
        }

        val nesteIterator = lagNesteIterator(schema, iterator, sorterteRaderMedRowId)

        val rader = sorterteRaderMedRowId.map { it.kolonner }
        return UttrekkResponse(
            iterator = nesteIterator.toDtoString(),
            schema = SchemaDto(
                kolonner = schema.kolonner.map { KolonnebeskrivelseDto(navn = it.navn.value) }
            ),
            innhold = rader
        )
    }

    private fun lagNesteIterator(
        schema: Schema,
        iterator: DatabaseIterator,
        sorterteRaderMedRowId: List<RadMedRowId>
    ): DatabaseIterator {
        val rowid = sorterteRaderMedRowId.lastOrNull()?.rowId
        val nesteIterator = rowid?.let { DatabaseIterator.RowidIterator(schema.tabellnavn, rowid) } ?: iterator
        return nesteIterator
    }

    override fun erKompatibelMed(schema: Schema): Boolean {
        return true // Dette er en fallback som skal fungere med alle tabeller
    }

    override val order: Int
        get() = Ordered.LOWEST_PRECEDENCE

    private fun finnRader(
        schema: Schema,
        antallRader: Long
    ): List<RadMedRowId> {
        val mapper = rowMapper(schema)

        val kolonner = schema.kolonner.map { it.navn }.joinToString(separator = ", ")

        val sql = "select $kolonner, ROWID from ${schema.tabellnavn.value} ORDER BY ROWID FETCH FIRST :antallRader ROWS ONLY"
        logger.debug("Kjører følgende SQL: $sql")

        val params = mapOf<String, Any>(
            "antallRader" to antallRader
        )
        val rader = jdbcTemplate.query(sql, params, mapper)
        return rader
    }

    private fun finnRaderStoerreEnn(
        schema: Schema,
        iterator: DatabaseIterator.RowidIterator,
        antallRader: Long
    ): List<RadMedRowId> {
        val mapper = rowMapper(schema)

        val kolonner = schema.kolonner.map { it.navn }.joinToString(separator = ", ")

        val sql = "select $kolonner, ROWID from ${schema.tabellnavn.value} where ROWID > :rowid ORDER BY ROWID FETCH FIRST :antallRader ROWS ONLY"
        val params = mapOf<String, Any>(
            "antallRader" to antallRader,
            "rowid" to iterator.rowid
        )
        val rader = jdbcTemplate.query(sql, params, mapper)
        return rader
    }

    private fun rowMapper(schema: Schema): RowMapper<RadMedRowId> {
        val mapper = RowMapper<RadMedRowId> { rs, _ ->
            val kolonner: List<String?> = schema.kolonner.mapIndexed { i, kolonne ->
                val idx = i + 1
                when (kolonne.type) {
                    SqlType.STRING -> rs.getString(idx)
                    SqlType.NUMBER -> rs.getBigDecimal(idx)?.toString()
                    SqlType.TIMESTAMP -> rs.getTimestamp(idx)?.toLocalDateTime()?.toString()
                    SqlType.DATE -> rs.getDate(idx)?.toLocalDate()?.toString()
                    SqlType.BINARY -> rs.getBytes(idx)?.toHexString()
                }
            }

            RadMedRowId(rs.getString("ROWID"), kolonner)
        }
        return mapper
    }
}

private fun ByteArray.toHexString(): String =
    this.joinToString(separator = "") { "%02x".format(it.toInt() and 0xFF) }

private data class RadMedRowId(val rowId: String, val kolonner: List<String?>)