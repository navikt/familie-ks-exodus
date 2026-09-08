package no.nav.historisk.innsyn.rest.controller

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
class OAuth2ErrorController {
    @GetMapping("/oauth2/**")
    fun oauth2() {
        throw SoekeException(Feilkode.SERVERFEIL, "Konfigurasjonsfeil",
            "Kan ikke logge inn uten Azure sidecar (wonderwall). " +
                    "Dersom du er saksbehandler og ser denne feilmeldingen så er det noe alvorlig galt med applikasjonen. Vennligst ta kontakt med support.")
    }
}