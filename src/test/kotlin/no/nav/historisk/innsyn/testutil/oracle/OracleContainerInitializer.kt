package no.nav.historisk.innsyn.testutil.oracle
import no.nav.historisk.innsyn.testutil.classpathString
import org.slf4j.LoggerFactory
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.testcontainers.containers.OracleContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

class OracleContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        if (!container.isRunning) {
            container.start()
            initializeSchema(container)
        }

        logger.info("Oracle JDBC URL: ${container.jdbcUrl}, user: ${container.username}, password: ${container.password}")
        TestPropertyValues.of(
            "spring.datasource.url=${container.jdbcUrl}",
            "spring.datasource.username=${container.username}",
            "spring.datasource.password=${container.password}"
        ).applyTo(applicationContext.environment)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OracleContainerInitializer::class.java)

        fun truncate() {
            logger.info("Truncating all tables")
            val conn = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
            val sql = classpathString("/truncateTables.sql")
            conn.createStatement().execute(sql)
            conn.close()
        }

        private fun initializeSchema(container: OracleContainer) {
            logger.info("Initializing schema")
            val conn = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)

            val p = ResourceDatabasePopulator()
            p.addScript(ClassPathResource("/testschema.sql"))
            p.populate(conn)

            conn.close()
        }

        // Lazy because we only want it to be initialized when accessed
        private val container: OracleContainer by lazy {
            OracleContainer( DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart")
                .asCompatibleSubstituteFor("gvenzl/oracle-xe"))
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")
        }
    }
}
