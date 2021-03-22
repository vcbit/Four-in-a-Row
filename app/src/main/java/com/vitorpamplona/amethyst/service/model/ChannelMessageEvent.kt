package com.vitorpamplona.amethyst.service.model

import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import nostr.postr.Utils
import java.util.Date

class ChannelMessageEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {

    fun channel() = tags.firstOrNull { it[0] == "e" && it.size > 3 && it[3] == "root" }?.getOrNull(1) ?: tags.firstOrNull { it.firstOrNull() == "e" }?.getOrNull(1)
    fun replyTos() = tags.filter { it.getOrNull(1) != channel() }.mapNotNull { it.getOrNull(1) }
    fun mentions() = tags.filter { it.firstOrNull() == "p" }.mapNotNull { it.getOrNull(1) }

    companion object {
        const val kind = 42

        fun create(message: String, channel: String, replyTos: List<String>? = null, mentions: List<String>? = null, privateKey: ByteArray, createdAt: Long = Date().time / 1000): ChannelMessageEvent {
            va