import no.nav.historisk.innsyn.Application
import no.nav.historisk.innsyn.testutil.oracle.OracleContainerInitializer
import org.springframework.boot.runApplication

fun main() {
    System.setProperty("oracle.jdbc.fanEnabled", "false")
    runApplication<Application>("--spring.profiles.active=dev,demoData,debug-sql") {
        this.addInitializers(OracleContainerInitializer())
    }
}