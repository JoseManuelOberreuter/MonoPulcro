package com.josem.monopulcro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DustMoteTest {

    @Test
    fun dustMotesForCount_zero_returnsEmpty() {
        assertEquals(0, dustMotesForCount(0).size)
    }

    @Test
    fun dustMotesForCount_five_returnsAllSlots() {
        val motes = dustMotesForCount(5)
        assertEquals(5, motes.size)
        assertEquals(DustMote.SLOTS, motes)
    }

    @Test
    fun dustMotesForCount_aboveMax_clampsToFive() {
        assertEquals(5, dustMotesForCount(99).size)
    }
}
