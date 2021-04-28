
package com.vitorpamplona.amethyst.service.nip19

import com.vitorpamplona.amethyst.model.toHexKey
import nostr.postr.bechToBytes

object Nip19 {
    enum class Type {
        USER, NOTE, RELAY, ADDRESS
    }

    data class Return(val type: Type, val hex: String, val relay: String? = null)

    fun uriToRoute(uri: String?): Return? {
        try {
            val key = uri?.removePrefix("nostr:") ?: return null

            val bytes = key.bechToBytes()
            if (key.startsWith("npub")) {
                return npub(bytes)
            } else if (key.startsWith("note")) {
                return note(bytes)
            } else if (key.startsWith("nprofile")) {
                return nprofile(bytes)
            } else if (key.startsWith("nevent")) {
                return nevent(bytes)
            } else if (key.startsWith("nrelay")) {
                return nrelay(bytes)
            } else if (key.startsWith("naddr")) {
                return naddr(bytes)
            }
        } catch (e: Throwable) {
            println("Issue trying to Decode NIP19 $uri: ${e.message}")
        }

        return null
    }

    private fun npub(bytes: ByteArray): Return {
        return Return(Type.USER, bytes.toHexKey())
    }

    private fun note(bytes: ByteArray): Return {
        return Return(Type.NOTE, bytes.toHexKey())
    }

    private fun nprofile(bytes: ByteArray): Return? {
        val tlv = Tlv.parse(bytes)

        val hex = tlv.get(Tlv.Type.SPECIAL.id)
            ?.get(0)
            ?.toHexKey() ?: return null

        val relay = tlv.get(Tlv.Type.RELAY.id)
            ?.get(0)
            ?.toString(Charsets.UTF_8)

        return Return(Type.USER, hex, relay)
    }

    private fun nevent(bytes: ByteArray): Return? {
        val tlv = Tlv.parse(bytes)

        val hex = tlv.get(Tlv.Type.SPECIAL.id)
            ?.get(0)
            ?.toHexKey() ?: return null

        val relay = tlv.get(Tlv.Type.RELAY.id)
            ?.get(0)
            ?.toString(Charsets.UTF_8)

        return Return(Type.USER, hex, relay)
    }

    private fun nrelay(bytes: ByteArray): Return? {
        val relayUrl = Tlv.parse(bytes)
            .get(Tlv.Type.SPECIAL.id)
            ?.get(0)
            ?.toString(Charsets.UTF_8) ?: return null

        return Return(Type.RELAY, relayUrl)
    }

    private fun naddr(bytes: ByteArray): Return? {
        val tlv = Tlv.parse(bytes)

        val d = tlv.get(Tlv.Type.SPECIAL.id)
            ?.get(0)
            ?.toString(Charsets.UTF_8) ?: return null

        val relay = tlv.get(Tlv.Type.RELAY.id)
            ?.get(0)
            ?.toString(Charsets.UTF_8)

        val author = tlv.get(Tlv.Type.AUTHOR.id)
            ?.get(0)
            ?.toHexKey()

        val kind = tlv.get(Tlv.Type.KIND.id)
            ?.get(0)
            ?.let { Tlv.toInt32(it) }

        return Return(Type.ADDRESS, "$kind:$author:$d", relay)
    }
}