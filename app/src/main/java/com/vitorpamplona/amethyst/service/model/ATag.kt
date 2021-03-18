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

