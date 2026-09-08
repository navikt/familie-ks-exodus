package no.nav.historisk.innsyn.utils

fun<T> timeMillis(block: () -> T): Pair<T, Long> {
    val t0 = System.nanoTime()
    val res = block()
    val t1 = System.nanoTime()
    return Pair(res, (t1 - t0) / 1_000_000)
}