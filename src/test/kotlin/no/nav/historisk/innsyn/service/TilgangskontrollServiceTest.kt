package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.utils.TokenHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    classes = [ TilgangskontrollService::class ],
    properties = [
        "app.gruppe.admin=gruppeAdmin"
    ]
)
@ActiveProfiles("test")
internal class TilgangskontrollServiceTest {
    @Autowired
    private lateinit var tilgangskontrollService: TilgangskontrollService

    @MockitoBean
    private lateinit var tokenHelper: TokenHelper

    @Test
    internal fun `skal ikke kaste exception dersom klient har tilgang til tabell`() {
        `when`(tokenHelper.roller()).thenReturn(listOf("tabellnavn"))
        tilgangskontrollService.validerTilgangTilTabell(Tabellnavn("tabellnavn"))
    }

    @Test
    internal fun `skal kaste exception dersom klient ikke har tilgang til tabell`() {
        `when`(tokenHelper.roller()).thenReturn(listOf("tabellnavn"))

        val e = assertThrows<SoekeException> { tilgangskontrollService.validerTilgangTilTabell(Tabellnavn("ikke_tilgang")) }
        assertThat(e.feilkode).isEqualTo(Feilkode.IKKE_TILGANG_TIL_TABELL)
    }
}