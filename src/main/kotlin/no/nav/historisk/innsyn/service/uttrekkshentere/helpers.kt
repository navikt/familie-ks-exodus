package no.nav.historisk.innsyn.service.uttrekkshentere

import no.nav.historisk.innsyn.model.KolonneSchema
import no.nav.historisk.innsyn.model.Schema
import no.nav.historisk.innsyn.utils.findExactlyOne

fun Schema.kolonne(navn: String): KolonneSchemaMedIndex {
    return this.kolonner.mapIndexed { i, k -> KolonneSchemaMedIndex(kolonne = k, index = i) }
        .findExactlyOne("Forventet å finne nøyaktig én kolonne med navn '$navn'") { it.kolonne.navn.value == navn }
}

fun Schema.idKolonne(): KolonneSchemaMedIndex {
    return this.kolonner.mapIndexed { i, k -> KolonneSchemaMedIndex(kolonne = k, index = i) }
        .findExactlyOne("Forventet å finne nøyaktig én kolonne som starter med ID_") { it.kolonne.navn.value.startsWith("ID_") }
}

data class KolonneSchemaMedIndex(val kolonne: KolonneSchema, val index: Int)