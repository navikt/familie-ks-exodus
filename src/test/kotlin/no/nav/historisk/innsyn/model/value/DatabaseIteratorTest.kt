package no.nav.historisk.innsyn.model.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DatabaseIteratorTest {
    @Test
    fun idOppdatertIterator() {
        val iterator = DatabaseIterator.IdOppdatertIterator(
            tabellnavn = Tabellnavn("tabellnavn"),
            tidspunktForSisteBaseline = LocalDate.of(1999, 1, 1).atStartOfDay(),
            sisteId = mapOf(
                Kolonnenavn("a") to "1",
                Kolonnenavn("b") to "xyz"
            )
        )
        val encoded = iterator.toDtoString()
        println(encoded)
        val parsed = DatabaseIterator.parse(encoded)
        assertThat(parsed).isEqualTo(iterator)
    }

    @Test
    internal fun nullIterator() {
        val iterator = DatabaseIterator.NullIterator(Tabellnavn("tabellnavn"))
        val encoded = iterator.toDtoString()
        val parsed = DatabaseIterator.parse(encoded)
        assertThat(parsed).isEqualTo(iterator)
    }

    @Test
    internal fun rowidIterator() {
        val iterator = DatabaseIterator.RowidIterator(Tabellnavn("tabellnavn"), "blah, blah, blah")
        val encoded = iterator.toDtoString()
        val parsed = DatabaseIterator.parse(encoded)
        assertThat(parsed).isEqualTo(iterator)
    }
}