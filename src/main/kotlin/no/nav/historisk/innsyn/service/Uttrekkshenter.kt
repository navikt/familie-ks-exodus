package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.dto.UttrekkResponse
import no.nav.historisk.innsyn.model.Schema
import no.nav.historisk.innsyn.model.value.DatabaseIterator

interface Uttrekkshenter {
    fun hentUttrekk(
        schema: Schema,
        iterator: DatabaseIterator,
        antallRader: Long
    ): UttrekkResponse

    fun erKompatibelMed(schema: Schema): Boolean
    
    val order: Int
        get() = 0
}