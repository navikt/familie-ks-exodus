package no.nav.historisk.innsyn.utils

fun<T> T.isOneOf(vararg verdier: T): Boolean {
    for (v in verdier) {
        if (v == this) {
            return true
        }
    }
    return false
}