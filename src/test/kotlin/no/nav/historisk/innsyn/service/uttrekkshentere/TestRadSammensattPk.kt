package no.nav.historisk.innsyn.service.uttrekkshentere

import java.time.LocalDateTime

data class TestRadSammensattPk(
    val value: Long,
    val id1: Long,
    val id2: Long,
    val id3: Long,
    val oppdatert: LocalDateTime,
)