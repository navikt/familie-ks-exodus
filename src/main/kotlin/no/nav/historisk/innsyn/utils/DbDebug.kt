package no.nav.historisk.innsyn.utils

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DbDebug(
    @Value("\${spring.datasource.url}") val url: String,
    @Value("\${spring.datasource.username}") val username: String,
    @Value("\${spring.datasource.password}") val password: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun log() {
        logger.info("Username: $username, Password len: ${password.length}, URL: $url")
    }
}