package no.nav.historisk.innsyn.dto

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import no.nav.historisk.innsyn.utils.timeMillis
import org.junit.Test

class JsonTest {
    val objectMapper = jacksonObjectMapper()

    @Test
    fun xx() {
        val response = UttrekkResponse(
            iterator = "xxxyy28u92384",
            schema = SchemaDto(
                kolonner = listOf(
                    KolonnebeskrivelseDto(navn = "Kolonne1"),
                    KolonnebeskrivelseDto(navn = "Kolonne1"),
                    KolonnebeskrivelseDto(navn = "Kolonne1")
                )
            ),
            innhold = listOf(
                listOf("xx", "yy", "zz"),
                listOf("xx", "yy", "zz"),
                listOf("xx", "yy", "zz"),
                listOf("xx", "yy", "zz"),
            )
        )

        val resultat = objectMapper.writeValueAsString(response)
        println(resultat)
    }

    @Test
    fun taTiden() {
        val antallKolonner = 20
        val antallRader = 1000

        val response = UttrekkResponse(
            iterator = "x",
            schema = SchemaDto(
                kolonner = (0 until antallKolonner).map {
                    KolonnebeskrivelseDto("Kolonne_$it")
                }
            ),
            innhold = (0 until antallRader).map { rad ->
                (0 until antallKolonner).map { kolonne ->
                    "Rad: $rad, kolonne: $kolonne"
                }
            }
        )

        for (i in 0 until 100) {
            readWrite(response)
        }
    }

    private fun readWrite(response: UttrekkResponse) {
        val (bytes, millisWrite) = timeMillis {
            objectMapper.writeValueAsBytes(response)
        }

        val (res, millisRead) = timeMillis {
            objectMapper.readValue<UttrekkResponse>(bytes)
        }

        println("read: $millisRead ms, write: $millisWrite ms, ${bytes.size} bytes")
    }
}