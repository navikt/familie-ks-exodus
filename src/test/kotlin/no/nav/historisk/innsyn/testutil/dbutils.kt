package no.nav.historisk.innsyn.testutil

import net.ttddyy.dsproxy.QueryCountHolder
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

fun assertSelectCount(count: Int) {
    // SQLStatementCountValidator.assertSelectCount(int) ender med NoSuchMethodError
    val queryCount = QueryCountHolder.getGrandTotal()
    Assertions.assertThat(queryCount.select).isEqualTo(count)
}

@TestConfiguration
class QueryCountListenerTestConfig {
    @Bean
    fun postProcessor(): BeanPostProcessor {
        return DataSourcePostProcessor()
    }

    private class DataSourcePostProcessor : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
            if(bean is DataSource) {
                return createProxy(bean)
            }
            return bean
        }

        fun createProxy(original: DataSource): DataSource {
            return ProxyDataSourceBuilder
                .create(original)
                .name("DataSourceProxy-QueryCountListener")
                .listener(DataSourceQueryCountListener())
                .build()
        }
    }
}