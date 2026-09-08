package no.nav.historisk.innsyn.model.value

data class Kolonnenavn(val value: String) {
    init {
        require(value.matches("^[a-zA-Z0-9_]+\$".toRegex())) { "Ikke et gyldig kolonnenavn: $value" }
    }

    override fun toString(): String {
        return value
    }
}
