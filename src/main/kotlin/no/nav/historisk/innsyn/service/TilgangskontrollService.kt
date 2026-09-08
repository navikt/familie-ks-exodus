package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.utils.TokenHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TilgangskontrollService(
    private val tokenHelper: TokenHelper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun validerTilgangTilTabell(tabell: Tabellnavn) {
        if (!harTilgangTilTabell(tabell)) {
            logger.info("Mangler tilgang til tabell: $tabell.")
            throw SoekeException(Feilkode.IKKE_TILGANG_TIL_TABELL, "Mangler tilgang til tabell: $tabell", "Sjekk konfigurasjon på Exodus")
        }
    }

    private fun harTilgangTilTabell(tabell: Tabellnavn): Boolean {
        // Dette konfigureres per klient i nais.yml-filen, se README.md

        val roller = tokenHelper.roller()
        logger.debug("Fikk følgende roller fra Azure: $roller")
        return roller.contains(tabell.value.lowercase())
    }
}