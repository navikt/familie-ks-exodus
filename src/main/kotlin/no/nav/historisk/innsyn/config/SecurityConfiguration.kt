package no.nav.historisk.innsyn.config

import no.nav.historisk.innsyn.Profiles
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox", "org.springdoc"])
@Profile("!${Profiles.NOAUTH}")
@Configuration
class SecurityConfiguration
