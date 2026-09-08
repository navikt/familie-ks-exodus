package no.nav.historisk.innsyn.service.uttrekkshentere

import java.time.LocalDateTime

data class TestRad(
    val id: Long,
    val oppdatert: LocalDateTime,
    val personKey: Long
)
