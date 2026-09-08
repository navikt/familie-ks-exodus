package no.nav.historisk.innsyn.utils

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenHelper(private val ctxHolder: TokenValidationContextHolder) {
    fun grupper(): List<String> {
        val oidcValidationContext: TokenValidationContext = ctxHolder.getTokenValidationContext()
        return oidcValidationContext.getClaims(AzureIssuer).getAsList("groups")?:
            throw IllegalStateException("Token mangler claim \"groups\"")
    }

    fun roller(): List<String> {
        val oidcValidationContext: TokenValidationContext = ctxHolder.getTokenValidationContext()
        return oidcValidationContext.getClaims(AzureIssuer).getAsList("roles")?:
        throw IllegalStateException("Token mangler claim \"roles\"")
    }

    fun azureClientIds() : Set<String> {
        val oidcValidationContext: TokenValidationContext = ctxHolder.getTokenValidationContext()

        return oidcValidationContext.issuers
            .filter { it == AzureIssuer }
            .map { oidcValidationContext.getClaims(it) }
            .filterNotNull()
            .filter { it.get(AzureV2ClientIdClaim) != null || it.get(AzureV1ClientIdClaim) != null }
            .map { "${it.get(AzureV2ClientIdClaim)?:it.get(AzureV1ClientIdClaim)!!}" }
            .toSet()
    }

    companion object {
        const val AzureIssuer = "azure"
        // https://docs.microsoft.com/en-us/azure/active-directory/develop/access-tokens#payload-claims
        private const val AzureV1ClientIdClaim = "appid"
        private const val AzureV2ClientIdClaim = "azp"
    }
}