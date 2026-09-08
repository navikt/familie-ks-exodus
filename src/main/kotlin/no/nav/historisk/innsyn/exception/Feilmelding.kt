package no.nav.historisk.innsyn.exception

import no.nav.historisk.innsyn.utils.MdcHelper

data class Feilmelding(
    val kode: String,
    val beskrivelse: String,
    val detaljertBeskrivelse: String
) {
    val callId: String? = MdcHelper.callId
}