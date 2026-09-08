package no.nav.historisk.innsyn.testutil.annotation

import no.nav.historisk.innsyn.testutil.oracle.OracleContainerInitializer
import no.nav.historisk.innsyn.testutil.oracle.OracleContainerTruncateExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [OracleContainerInitializer::class])
@ExtendWith(OracleContainerTruncateExtension::class)
//@ActiveProfiles(Profiles.NO_LOS, Profiles.NO_KAFKA, Profiles.NO_VAULT, Profiles.CONSOLE_LOGGING, StubMeldingMetadataProvider.PROFILE)
annotation class IntegrationTest



// todo: set spring.datasource.initialization-mode=never, og initialiser schema fra Initializer...