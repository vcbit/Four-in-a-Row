package com.vitorpamplona.amethyst.service.nip19

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Tlv {
    enum class Type(val id: Byte) {
        SPECIAL(0),
        RELAY(1),
        AUTHOR(2),
        KIND(3);
    }

    fun toInt32(bytes: ByteArray): Int {
        require(bytes.size == 4) { "length must be 4, got: ${bytes.size}" }
        return Byt