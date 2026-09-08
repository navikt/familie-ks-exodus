package no.nav.historisk.innsyn.repository

import no.nav.historisk.innsyn.model.value.Kolonnenavn
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.testutil.annotation.RepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@RepositoryTest
class SchemaRepositoryTest {
    @Autowired
    private lateinit var schemaRepository: SchemaRepository

    @Test
    fun `finnPrimaryKey skal returnere alle kolonnenavnene som tilhører primary key`() {
        val tabellnavn = Tabellnavn("test_sammensatt_pk")
        val res = schemaRepository.finnPrimaryKey(tabellnavn)
        assertThat(res).containsExactly(
            Kolonnenavn("ID_1"),
            Kolonnenavn("ID_2"),
            Kolonnenavn("ID_3"),
        )
    }
}