package no.nav.historisk.innsyn.rest.controller

import no.nav.historisk.innsyn.dto.CountRequest
import no.nav.historisk.innsyn.dto.CountResponse
import no.nav.historisk.innsyn.dto.UttrekkRequest
import no.nav.historisk.innsyn.dto.UttrekkResponse
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.service.TilgangskontrollService
import no.nav.historisk.innsyn.service.UttrekkService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class UttrekkController(
    private val uttrekkService: UttrekkService,
    private val tilgangskontrollService: TilgangskontrollService
) {
    @PostMapping("/api/hentUttrekk")
    fun hentUttrekk(@RequestBody req: UttrekkRequest): UttrekkResponse {
        tilgangskontrollService.validerTilgangTilTabell(Tabellnavn(req.tabellnavn))
        return uttrekkService.hentUttrekk(req)
    }

    @PostMapping("/api/tellRader")
    fun tellRader(@RequestBody req: CountRequest): CountResponse {
        tilgangskontrollService.validerTilgangTilTabell(Tabellnavn(req.tabellnavn))
        return uttrekkService.tellRader(req)
    }
}