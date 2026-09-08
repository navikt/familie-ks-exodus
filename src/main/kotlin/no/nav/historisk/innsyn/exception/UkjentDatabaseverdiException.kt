package no.nav.historisk.innsyn.exception

import java.lang.RuntimeException

class UkjentDatabaseverdiException(val verdi: String, val gyldigeVerdier: List<String>)
    : RuntimeException("Ukjent databaseverdi '$verdi'. Tillatte verdier er: ${gyldigeVerdier.joinToString()}") {
}