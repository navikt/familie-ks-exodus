package no.nav.historisk.innsyn.utils

fun <T : Comparable<T>> maxOfOrNull(vararg values: T?): T? {
    var max: T? = null
    for (v in values) {
        if (max == null) {
            max = v
        } else if (v != null && v > max) {
            max = v
        }
    }
    return max
}

fun <T : Comparable<T>> minOfOrNull(vararg values: T?): T? {
    var min: T? = null
    for (v in values) {
        if (min == null) {
            min = v
        } else if (v != null && v < min) {
            min = v
        }
    }
    return min
}