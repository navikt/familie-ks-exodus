package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.dto.CountRequest
import no.nav.historisk.innsyn.dto.CountResponse
import no.nav.historisk.innsyn.dto.UttrekkRequest
import no.nav.historisk.innsyn.dto.UttrekkResponse
import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.exception.UttrekkException
import no.nav.historisk.innsyn.model.value.DatabaseIterator
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.repository.SchemaRepository
import no.nav.historisk.innsyn.utils.findExactlyOne
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class UttrekkService(
    private val schemaRepository: SchemaRepository,
    private val alleUttrekkshentere: List<Uttrekkshenter>,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val uttrekkshenterOverrides: UttrekkshenterOverrides
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val uttrekkshentere: List<Uttrekkshenter>
        get() = alleUttrekkshentere.sortedBy { it.order }

    fun hentUttrekk(req: UttrekkRequest): UttrekkResponse {
        val tabellnavn = Tabellnavn(req.tabellnavn)

        val schema = schemaRepository.finnSchema(tabellnavn)
            ?: throw SoekeException(
                feilkode = Feilkode.KLIENTFEIL,
                beskrivelse = "Ukjent tabell",
                detaljertBeskrivelse = "Tabellen \"$tabellnavn\" finnes ikke i databasen."
            )

        val override = uttrekkshenterOverrides.uttrekkshenter(tabellnavn)

        val uttrekkshenter = override ?: uttrekkshentere.find { it.erKompatibelMed(schema) }
            ?: throw SoekeException(
                feilkode = Feilkode.SERVERFEIL,
                beskrivelse = "Implementasjon mangler",
                detaljertBeskrivelse = "Det finnes ingen implementasjon av ${Uttrekkshenter::class.java.canonicalName} " +
                        "som er kompatibel med tabellen \"${schema.tabellnavn.value}\"."
            )

        logger.debug("Valgt følgende uttrekkshenter for tabell $tabellnavn: ${uttrekkshenter.javaClass.canonicalName}. Tilgjengelige uttrekkshentere: ${this.uttrekkshentere.map { it.javaClass.canonicalName }}")

        val iterator = lagIterator(req)

        return try {
            uttrekkshenter.hentUttrekk(schema, iterator, req.antallRader)
        } catch (e: SoekeException) {
            throw UttrekkException(
                feilkode = e.feilkode,
                tabellnavn = tabellnavn,
                beskrivelse = "${e.feilmelding.beskrivelse}\n${e.feilmelding.detaljertBeskrivelse}",
                cause = e
            )
        } catch (e: Exception) {
            throw UttrekkException(
                feilkode = Feilkode.SERVERFEIL,
                tabellnavn = tabellnavn,
                beskrivelse = "Kunne ikke hente uttrekk.",
                cause = e
            )
        }
    }

    private fun lagIterator(
        req: UttrekkRequest
    ): DatabaseIterator {
        val tabellnavn = Tabellnavn(req.tabellnavn)
        val opprinneligIterator: DatabaseIterator? = req.iterator?.let { DatabaseIterator.parse(it) }
        if (opprinneligIterator != null && opprinneligIterator.tabellnavn != tabellnavn) {
            throw SoekeException(
                Feilkode.KLIENTFEIL, "Feil bruk av iterator",
                "Iteratoren kan ikke gjenbrukes i ulike søk med forskjellige tabeller. Etterspurt tabell er '$tabellnavn', men iteratoren er laget for '${opprinneligIterator.tabellnavn}'"
            )
        }
        val iterator = opprinneligIterator ?: DatabaseIterator.NullIterator(tabellnavn)
        return iterator
    }

    fun tellRader(req: CountRequest): CountResponse {
        val tabellnavn = Tabellnavn(req.tabellnavn)
        val antall = try {
            jdbcTemplate.query("select count(1) as c from $tabellnavn") { row, _ -> row.getLong("c") }
                .findExactlyOne("Forventet å få nøyaktig én rad fra spørring.")
        } catch (e: Exception) {
            throw UttrekkException(
                feilkode = Feilkode.SERVERFEIL,
                tabellnavn = tabellnavn,
                beskrivelse = "Kunne ikke telle rader.",
                cause = e
            )
        }
        return CountResponse(antall = antall)
    }
}
