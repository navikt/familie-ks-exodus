package no.nav.historisk.innsyn.model

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.UkjentDatabaseverdiException
import no.nav.historisk.innsyn.exception.UttrekkException
import no.nav.historisk.innsyn.model.value.Kolonnenavn
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.utils.findAtMostOne

data class Schema(
    val tabellnavn: Tabellnavn,
    val kolonner: List<KolonneSchema>
) {
    val primaryKeyOrNull: List<KolonneSchema>?
        get() {
            val primaryKey = kolonner.filter { it.primaryKey }.sortedBy { it.navn.value }
            if (primaryKey.isEmpty()) {
                return null
            }
            return primaryKey
        }

    val primaryKey: List<KolonneSchema>
        get() = primaryKeyOrNull ?:
            throw UttrekkException(
                feilkode = Feilkode.SERVERFEIL,
                tabellnavn = tabellnavn,
                beskrivelse = "Tabell mangler primary key"
            )

    fun kolonne(navn: Kolonnenavn): KolonneSchema? {
        return kolonner.findAtMostOne("Forventet å finne maksimalt én kolonne med navn '$navn'") { it.navn == navn }
    }
}

data class KolonneSchema(
    val navn: Kolonnenavn,
    val type: SqlType,
    val primaryKey: Boolean,
    val index: Int
)

enum class SqlType {
    STRING,
    NUMBER,
    TIMESTAMP,
    DATE,
    BINARY;

    companion object {
        fun fromOracleString(str: String): SqlType {
            return when (str) {
                "CHAR" -> STRING
                "VARCHAR2" -> STRING
                "NUMBER" -> NUMBER // todo: bigint osv...
                "DATE" -> DATE
                "RAW" -> BINARY
                "LONG RAW" -> BINARY
                else -> {
                    if (str.startsWith("TIMESTAMP")) {
                        TIMESTAMP
                    } else {
                        throw UkjentDatabaseverdiException(str, emptyList())
                    }
                }
            }
        }
    }
}