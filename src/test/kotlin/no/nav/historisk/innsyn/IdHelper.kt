package no.nav.historisk.innsyn

import java.util.concurrent.atomic.AtomicLong

private val current = AtomicLong()

fun nextId(): Long = current.incrementAndGet()