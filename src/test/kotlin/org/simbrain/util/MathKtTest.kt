package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MathKtTest {

    @Test
    fun `test one hot matrix`() {
        var oneHot = getOneHot(2, 10)
        assertEquals(0.0, oneHot[0,0])
        assertEquals(1.0, oneHot[2,0])
        assertEquals(10L, oneHot.size())

        oneHot = getOneHot(2, 10, 2.0)
        assertEquals(2.0, oneHot[2,0])
    }

    @Test
    fun `test one hot array`() {
        var oneHot = getOneHotArray(2, 10)
        assertEquals(0.0, oneHot[0])
        assertEquals(1.0, oneHot[2])
        assertEquals(10, oneHot.size)

        oneHot = getOneHotArray(2, 10, 2.0)
        assertEquals(2.0, oneHot[2])
    }

    @Test
    fun `test geometric progression`() {
        val seq = createGeometricProgression(1.0, 2.0).takeWhile { it < 2000 }.toList()
        assertEquals(1.0, seq[0])
        assertEquals(2.0, seq[1])
        assertEquals(4.0, seq[2])
        assertEquals(8.0, seq[3])
        assertEquals(11, seq.size)
    }

    @Test
    fun `test vector sum`() {
        val array = listOf(doubleArrayOf(1.0,2.0), doubleArrayOf(-1.0,1.0))
        assertArrayEquals(doubleArrayOf(0.0, 3.0),array.sum())
    }
}

