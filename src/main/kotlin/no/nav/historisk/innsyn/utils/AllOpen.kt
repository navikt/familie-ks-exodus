package no.nav.historisk.innsyn.utils

/**
 * Annotering for å gjøre det mulig for Spring å extende klasser som ikke er
 * annotert med @Component eller @Service.
 *
 * https://kotlinlang.org/docs/reference/compiler-plugins.html#using-in-maven
 */
annotation class AllOpen