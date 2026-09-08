package no.nav.historisk.innsyn.rest.controller

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
class TableController() {
    @GetMapping(path = ["/tables"])
    fun get(): Map<String, List<String>> {
        TODO()
    }
}