package no.nav.historisk.innsyn

import no.nav.historisk.innsyn.testutil.oracle.OracleContainerInitializer
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles("test", "demoData")
@ContextConfiguration(initializers = [OracleContainerInitializer::class])
class DemoDataTest {

    @Test
    fun contextLoads() {
    }

}
