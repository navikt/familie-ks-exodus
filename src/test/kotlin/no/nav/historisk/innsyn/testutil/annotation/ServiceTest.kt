package no.nav.historisk.innsyn.testutil.annotation

import no.nav.historisk.innsyn.testutil.oracle.OracleContainerInitializer
import no.nav.historisk.innsyn.testutil.oracle.OracleContainerTruncateExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [OracleContainerInitializer::class])
@ExtendWith(OracleContainerTruncateExtension::class)
@ActiveProfiles("test")
annotation class ServiceTest