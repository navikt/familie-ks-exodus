package no.nav.historisk.innsyn.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class SoekeException(
    val feilkode: Feilkode,
    private val beskrivelse: String,
    private val detaljertBeskrivelse: String
) : ResponseStatusException(feilkode.status, beskrivelse) {
    val feilmelding: Feilmelding
        get() {
            return Feilmelding(
                kode = feilkode.kode,
                beskrivelse = beskrivelse,
                detaljertBeskrivelse = detaljertBeskrivelse
            )
        }

    val status: HttpStatus
        get() = feilkode.status
}