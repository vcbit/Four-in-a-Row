package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.service.nip19.Tlv
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class TlvTest {

    @Test(expected = IllegalArgumentException::class)
    fun to_int_32_length_smaller_than_4() {
        Tlv.toInt32(byteArrayOfInts(1, 2, 3))
    }

    @Test(expected = IllegalArgumentExceptio