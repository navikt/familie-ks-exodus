package no.nav.historisk.innsyn.exception

import org.springframework.http.HttpStatus

enum class Feilkode(val kode: String, val status: HttpStatus, val tilgangskontroll: Boolean = false) {
    IKKE_TILGANG_TIL_TABELL("ikke_tilgang_til_tabell", HttpStatus.FORBIDDEN, tilgangskontroll = true),
    IKKE_TILGANG_TIL_APPLIKASJON("ikke_tilgang_til_applikasjon", HttpStatus.FORBIDDEN, tilgangskontroll = true),
    NY_BASELINE("baseline", HttpStatus.CONFLICT),

    // Generiske feil
    SERVERFEIL("serverfeil", HttpStatus.INTERNAL_SERVER_ERROR),
    KLIENTFEIL("klientfeil", HttpStatus.BAD_REQUEST)
}