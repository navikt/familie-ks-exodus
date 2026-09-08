package no.nav.historisk.innsyn.model.value

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

internal class TabellnavnTest {
    @Test
    fun tabellnavn() {
        Tabellnavn("Tabellnavn_123")
        ikkeGyldig("")
        ikkeGyldig(" ")
        ikkeGyldig("tabell navn")
        ikkeGyldig(";asdf")
        ikkeGyldig("tabell-navn")
    }

    private fun ikkeGyldig(value: String) {
        assertThrows<IllegalArgumentException> {
            Tabellnavn(value)
        }
    }
}