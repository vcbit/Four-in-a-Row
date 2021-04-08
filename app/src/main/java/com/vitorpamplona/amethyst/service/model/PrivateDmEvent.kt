package com.vitorpamplona.amethyst.service.model

import android.util.Log
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import fr.acinq.secp256k1.Hex
import nostr.postr.Utils
import nostr.postr.toHex
import java.util.Date

class PrivateDmEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {
    /**
     * This may or may not be the actual recipient's pub key. The event is intended to look like 