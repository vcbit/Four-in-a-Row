package com.vitorpamplona.amethyst.service.model

import android.util.Log
import com.vitorpamplona.amethyst.model.toByteArray
import com.vitorpamplona.amethyst.model.toHexKey
import com.vitorpamplona.amethyst.service.nip19.Tlv
import fr.acinq.secp256k1.Hex
import nostr.postr.Bech32
import nostr.postr.bechToBytes
import nostr.postr.toByteArray

data class ATag(val kind: Int, val pubKeyHex: String, val dTag: String, val relay: String?) {
    fun toTag() = "$kind:$pubKeyHex:$dTag"

    fun toNAddr(): String {
        val kind = kind.toByteArray()
        val author = pubKeyHex.toByteArray()
        val dTag = dTag.toByteArray(Charsets.UTF_8)
        val relay = relay?.toByteArray(Charsets.UTF_8)

        var fullArray = byteArrayOf(Tlv.Type.SPECIAL.id, dTag.size.toByte()) + dTag

        if (relay != null) {
            fullArray = fullArray + byteArrayOf(Tlv.Type.RELAY.id, relay.size.toByte()) + relay
        }

        fullArray = fullArray +
            byteArrayOf(Tlv.Type.AUTHOR.id, author.size.toByte()) + author +
            byteArrayOf(Tlv.Type.KIND.id, kind.size.toByte()) + kind

        return Bech32.encodeBytes(hrp = "naddr", fullArray, Bech32.Encoding.Bech32)
    }

    companion object {
        fun isATag(key: String): Boolean {
            return key.startsWith("naddr1") || key.contains(":")
        }

        fun parse(address: String, relay: String?): ATag? {
            return if (address.startsWith("naddr") || address.startsWith("nostr:naddr")) {
                parseNAddr(address)
            } else {
                parseAtag(address, relay)
            }
        }

        fun parseAtag(atag: String, relay: String?): ATag? {
            return 