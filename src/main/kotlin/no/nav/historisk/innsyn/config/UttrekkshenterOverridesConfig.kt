package no.nav.historisk.innsyn.config

import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.service.Uttrekkshenter
import no.nav.historisk.innsyn.service.UttrekkshenterOverrides
import no.nav.historisk.innsyn.service.uttrekkshentere.IdUttrekkshenter
import no.nav.historisk.innsyn.service.uttrekkshentere.RowidUttrekkshenter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UttrekkshenterOverridesConfig(
    @Value("\${app.uttrekkshentere.rowid}")
    private val rowidOverrides: String,
    private val rowidUttrekkshenter: RowidUttrekkshenter,

    @Value("\${app.uttrekkshentere.id}")
    private val idOverrides: String,
    private val idUttrekkshenter: IdUttrekkshenter
) {
    @Bean
    fun uttrekkshenterOverrides(): UttrekkshenterOverrides {
        val uttrekkshentere: MutableMap<Tabellnavn, Uttrekkshenter> = mutableMapOf()

        for (tabellnavn in rowidOverrides.split(',').map { it.trim() }) {
            if (tabellnavn.isNotBlank()) {
                uttrekkshentere[Tabellnavn(tabellnavn.lowercase())] = rowidUttrekkshenter
            }
        }

        for (tabellnavn in idOverrides.split(',').map { it.trim() }) {
            if (tabellnavn.isNotBlank()) {
                uttrekkshentere[Tabellnavn(tabellnavn.lowercase())] = idUttrekkshenter
            }
        }

        return UttrekkshenterOverrides(uttrekkshentere)
    }
}