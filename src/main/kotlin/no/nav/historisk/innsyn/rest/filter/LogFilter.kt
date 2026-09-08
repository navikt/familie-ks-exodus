package no.nav.historisk.innsyn.rest.filter

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.historisk.innsyn.utils.MdcHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.util.*
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Component
@Order(LOWEST_PRECEDENCE)
class LogFilter(private val registry: MeterRegistry, @Value("\${spring.application.name}") private val applicationName: String) : GenericFilterBean() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val dontLog = setOf(
        "/actuator/health",
        "/actuator/prometheus",
        "/actuator")

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        putValues(HttpServletRequest::class.java.cast(request))
        val req = request as HttpServletRequest
        val res = response as HttpServletResponse

        val frontendLoggingCookie = "frontend_logging_id"
        val frontendLogUuid: String = req.cookies?.find { it.name == frontendLoggingCookie }?.value
            ?: run {
                val uuid = UUID.randomUUID().toString()
                val cookie = Cookie(frontendLoggingCookie, uuid)
                cookie.setPath("/")
                cookie.setSecure(true)
                cookie.setHttpOnly(true)
                res.addCookie(cookie)
                uuid
            }

        MdcHelper.frontendLogUuid = frontendLogUuid

        try {
            val millis = time { chain.doFilter(request, response) }

            if(!dontLog.contains(req.requestURI)) {
                val host = req.getHeader("Host")
                log.info("[${millis}ms]\t${res.status} ${req.method} ${req.requestURI} \t($host)")
            }

        } finally {
            MdcHelper.clear()
        }
    }

    private fun time(block: () -> Unit): Long {
        val t0 = System.nanoTime()
        block()
        val t1 = System.nanoTime()
        return (t1 - t0) / 1_000_000
    }

    private fun putValues(request: HttpServletRequest) {
        try {
            val consumerId = request.getHeader(CONSUMER_ID_HEADER) ?: "[ukjent_applikasjon]"
            MdcHelper.consumerId = consumerId
            MdcHelper.callId = request.getHeader(CALL_ID_HEADER) ?: UUID.randomUUID().toString()
            registry.counter("${applicationName}_consumers", listOf(Tag.of("consumer_id", consumerId))).increment()
        } catch (e: Exception) {
            log.warn("Noe gikk galt ved setting av MDC-verdier for request {}, MDC-verdier er inkomplette", request.requestURI, e)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    companion object {
        const private val CONSUMER_ID_HEADER = "Nav-Consumer-Id"
        const private val CALL_ID_HEADER = "Nav-CallId"
    }
}
