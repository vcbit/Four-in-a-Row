package com.vitorpamplona.amethyst.service.model

import android.util.Log
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import nostr.postr.Utils
import java.util.Date

class ChannelCreateEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {
    fun channelInfo() = try {
        MetadataEvent.gson.fromJson(content, ChannelData::class.java)
    } catch (e: Exception) {
        Log.e("ChannelMetadataEvent", "Can't parse channel info $content", e)
        ChannelData(null, null, null)
    }

    companion object {
        const val kind = 40

        fun create(channelInfo: ChannelData?, privateKey: ByteArray, createdAt: