package no.nav.historisk.innsyn.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MaxMinOrNullKtTest {

    @Test
    fun `maxOrNull finner max`() {
        val res = maxOfOrNull(null, 1, 2, 3, null)
        assertThat(res).isEqualTo(3)

        assertThat(maxOfOrNull<Int>()).isNull()
        assertThat(maxOfOrNull<Int>(null)).isNull()
    }

    @Test
    fun `minOrNull finner min`() {
        val res = minOfOrNull(null, 1, 2, 3, null)
        assertThat(res).isEqualTo(1)

        assertThat(minOfOrNull<Int>()).isNull()
        assertThat(minOfOrNull<Int>(null)).isNull()
    }
}