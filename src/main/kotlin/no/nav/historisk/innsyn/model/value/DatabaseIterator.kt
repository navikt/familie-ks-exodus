package no.nav.historisk.innsyn.model.value

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.Base64

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(value = DatabaseIterator.IdOppdatertIterator::class, name = "IdOppdatertIterator"),
    JsonSubTypes.Type(value = DatabaseIterator.RowidIterator::class, name = "RowidIterator"),
    JsonSubTypes.Type(value = DatabaseIterator.NullIterator::class, name = "NullIterator")
)
sealed interface DatabaseIterator {
    companion object {
        private fun encode(iterator: DatabaseIterator): String {
            return String(Base64.getEncoder().encode(OBJECT_MAPPER.writeValueAsBytes(iterator)))
        }

        fun parse(str: String): DatabaseIterator {
            return OBJECT_MAPPER.readValue(Base64.getDecoder().decode(str))
        }

        private val OBJECT_MAPPER = jacksonObjectMapper()
    }

    fun toDtoString(): String = encode(this)
    val tabellnavn: Tabellnavn

    data class IdOppdatertIterator(
        override val tabellnavn: Tabellnavn,
        val tidspunktForSisteBaseline: LocalDateTime?,
        val sisteId: Map<Kolonnenavn, String>
    ) : DatabaseIterator

    data class IdIterator(
        override val tabellnavn: Tabellnavn,
        val tidspunktForSisteBaseline: LocalDateTime?,
        val sisteId: String
    ) : DatabaseIterator

    data class RowidIterator(override val tabellnavn: Tabellnavn, val rowid: String) : DatabaseIterator
    data class NullIterator(override val tabellnavn: Tabellnavn) : DatabaseIterator
}