package no.nav.historisk.innsyn.testutil.demodata

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
@Profile("demoData")
class DemoData(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        val inserts = listOf(
            "insert into TEST_OPPDATERT (PS01_PERSONKEY) values (99123123123)",
            "insert into TEST_OPPDATERT (PS01_PERSONKEY) values (23432423434)",

            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (123)",
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (456)",
            "insert into TEST_INGEN_TIMESTAMP (PS01_PERSONKEY) values (789)"
        )
        inserts.forEach {
            jdbcTemplate.jdbcTemplate.execute(it)
        }
    }
}