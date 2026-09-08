package no.nav.historisk.innsyn.utils

import org.slf4j.MDC

object MdcHelper {
    const val CONSUMER_ID_HEADER = "Nav-Consumer-Id"
    const val CALL_ID_HEADER = "Nav-CallId"
    const val FRONTEND_LOG_UUID = "frontendLogUuid"

    var callId: String?
        get() = MDC.get(CALL_ID_HEADER)
        set(value) = MDC.put(CALL_ID_HEADER, value)

    var consumerId: String?
        get() = MDC.get(CONSUMER_ID_HEADER)
        set(value) = MDC.put(CONSUMER_ID_HEADER, value)

    var frontendLogUuid: String?
        get() = MDC.get(FRONTEND_LOG_UUID)
        set(value) = MDC.put(FRONTEND_LOG_UUID, value)

    fun clear() {
        MDC.clear()
    }
}