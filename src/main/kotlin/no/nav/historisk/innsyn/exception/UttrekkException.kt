package no.nav.historisk.innsyn.exception

import no.nav.historisk.innsyn.model.value.Tabellnavn
import org.springframework.web.server.ResponseStatusException

class UttrekkException(
    val feilkode: Feilkode,
    val tabellnavn: Tabellnavn,
    val beskrivelse: String,
    cause: Throwable? = null
) : ResponseStatusException(feilkode.status, "[$tabellnavn] $beskrivelse", cause)
