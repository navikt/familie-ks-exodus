package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.model.value.Tabellnavn

class UttrekkshenterOverrides(private val overrides: Map<Tabellnavn, Uttrekkshenter>) {
    fun uttrekkshenter(tabellnavn: Tabellnavn): Uttrekkshenter?
        = overrides[Tabellnavn(tabellnavn.value.lowercase())]
}