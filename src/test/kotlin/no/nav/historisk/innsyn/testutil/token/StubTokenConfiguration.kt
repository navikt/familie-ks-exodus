package no.nav.historisk.innsyn.testutil.token

import com.nimbusds.jwt.JWT
import no.nav.TestProfiles
import no.nav.historisk.innsyn.SecurityConfigurationDev
import no.nav.historisk.innsyn.utils.TokenHelper
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.*

@Configuration
@Import(SecurityConfigurationDev::class)
@Profile(TestProfiles.STUB_TOKEN_CONTEXT)
class StubTokenConfiguration {
    @Autowired
    private lateinit var server: MockOAuth2Server

    @Primary
    @Bean
    fun tokenValidationContextHolder(): TokenValidationContextHolder {
        val holder = mock(TokenValidationContextHolder::class.java)
        Mockito.`when`(holder.getTokenValidationContext())
            .thenReturn(tokenValidationContext())
        return holder
    }

    private fun tokenValidationContext(): TokenValidationContext {
        val accessToken = accessToken()
        val map: MutableMap<String, JwtToken> = HashMap()
        map[TokenHelper.AzureIssuer] = JwtToken(accessToken.serialize())
        return TokenValidationContext(map)
    }

    private fun accessToken(): JWT {
        return server.issueToken(TokenHelper.AzureIssuer, audience = "test")
    }
}