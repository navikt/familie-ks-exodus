package no.nav.historisk.innsyn.dto

class UttrekkRequest(
    val tabellnavn: String,
    val iterator: String?,
    val antallRader: Long
)

data class UttrekkResponse(
    val iterator: String,
    val schema: SchemaDto,
    val innhold: List<List<String?>>
)

data class SchemaDto(
    val kolonner: List<KolonnebeskrivelseDto>
)

data class KolonnebeskrivelseDto(
    val navn: String
)

data class CountRequest(val tabellnavn: String)
data class CountResponse(val antall: Long)