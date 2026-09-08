package no.nav.historisk.innsyn
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

class LogFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent?): FilterReply {
        if(event == null) {
            return FilterReply.NEUTRAL
        }

        if (!event.loggerName.startsWith("no.nav") && event.level.levelInt < Level.INFO.levelInt) {
            return FilterReply.DENY
        }

        return FilterReply.NEUTRAL
    }
}