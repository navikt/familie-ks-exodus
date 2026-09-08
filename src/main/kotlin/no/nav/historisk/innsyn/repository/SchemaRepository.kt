package no.nav.historisk.innsyn.repository

import no.nav.historisk.innsyn.model.KolonneSchema
import no.nav.historisk.innsyn.model.Schema
import no.nav.historisk.innsyn.model.SqlType
import no.nav.historisk.innsyn.model.value.Kolonnenavn
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.utils.timeMillis
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SchemaRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun finnSchema(tabellnavn: Tabellnavn): Schema? {
        val primaryKey = finnPrimaryKey(tabellnavn)

        val mapper = RowMapper<KolonneSchema> { rs, rn ->
            val kolonnenavn = Kolonnenavn(rs.getString("COLUMN_NAME"))
            KolonneSchema(
                navn = kolonnenavn,
                type = rs.getString("DATA_TYPE").let(SqlType.Companion::fromOracleString),
                primaryKey = primaryKey?.contains(kolonnenavn) ?: false,
                index = rn
            )
        }

        val sql = """select COLUMN_NAME, DATA_TYPE from ALL_TAB_COLUMNS where TABLE_NAME = :tableName"""
        val params = mapOf("tableName" to tabellnavn.value.uppercase())

        val (kolonner, millis) = timeMillis {
            jdbcTemplate.query(
                sql,
                params, mapper
            )
        }

        logger.debug("finnSchema: Kjørte følgende SQL (tid: {}ms, params: {}): {}", millis, params, sql)

        if (kolonner.isEmpty()) {
            return null
        }

        val schema = Schema(
            tabellnavn = tabellnavn,
            kolonner = kolonner
        )
        return schema
    }

    fun finnPrimaryKey(tabellnavn: Tabellnavn): List<Kolonnenavn>? {
        val mapper = RowMapper<Kolonnenavn> { rs, _ ->
            Kolonnenavn(rs.getString("COLUMN_NAME"))
        }

        // language=sql
        val sql: String = """
            select columns.column_name
            from all_cons_columns columns
            inner join all_constraints constraints on (
                constraints.constraint_name = columns.constraint_name
                    AND constraints.owner = columns.owner
                )
            where columns.table_name = :tableName
              AND constraints.constraint_type = 'P'
        """.trimIndent()

        val (kolonner, millis) = timeMillis {
            jdbcTemplate.query(
                sql,
                mapOf("tableName" to tabellnavn.value.uppercase()), mapper
            )
        }

        logger.debug("finnPrimaryKey: Kjørte følgende SQL (tid: {}ms): {}", millis, sql)

        if (kolonner.isEmpty()) {
            return null
        }

        return kolonner.sortedBy { it.value }
    }
}