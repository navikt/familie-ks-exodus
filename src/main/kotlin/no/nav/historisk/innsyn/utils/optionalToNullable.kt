package no.nav.historisk.innsyn.utils

import java.util.*

fun <T> Optional<T>.toNullable(): T? {
    return if (this.isPresent) this.get()
    else null
}