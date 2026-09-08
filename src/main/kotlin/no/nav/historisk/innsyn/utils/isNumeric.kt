package no.nav.historisk.innsyn.utils

fun String.isNumeric(): Boolean {
    return this.trim().toIntOrNull() != null
}