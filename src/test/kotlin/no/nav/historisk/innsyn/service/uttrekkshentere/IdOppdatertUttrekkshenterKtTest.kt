package no.nav.historisk.innsyn.service.uttrekkshentere

import no.nav.historisk.innsyn.model.value.Kolonnenavn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IdOppdatertUttrekkshenterKtTest {

    @Test
    fun `whereklausul blir rett med flere kolonner`() {
        val input = listOf(
            Kolonnenavn("a"),
            Kolonnenavn("b"),
            Kolonnenavn("c"),
        )
        val res = whereKlausul(input)
        assertThat(res).isEqualTo("a = :a\n and b = :b\n and c > :c")
    }

    @Test
    fun `whereklausul blir rett med bare en kolonne`() {
        val input = listOf(
            Kolonnenavn("c"),
        )
        val res = whereKlausul(input)
        assertThat(res).isEqualTo("c > :c")
    }
}