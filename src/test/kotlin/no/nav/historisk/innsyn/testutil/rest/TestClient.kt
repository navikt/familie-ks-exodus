package no.nav.historisk.innsyn.testutil.rest

import no.nav.historisk.innsyn.dto.CountRequest
import no.nav.historisk.innsyn.dto.CountResponse
import no.nav.historisk.innsyn.dto.UttrekkRequest
import no.nav.historisk.innsyn.dto.UttrekkResponse
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject

class TestClient(private val restTemplate: RestTemplate) {
    fun hentUttrekk(req: UttrekkRequest): UttrekkResponse {
        return restTemplate.postForObject<UttrekkResponse>("/api/hentUttrekk", req) !!
    }

    fun tellRader(req: CountRequest): CountResponse {
        return restTemplate.postForObject<CountResponse>("/api/tellRader", req) !!
    }
}