package com.vitorpamplona.amethyst.model

import androidx.lifecycle.LiveData
import com.vitorpamplona.amethyst.service.NostrSingleChannelDataSource
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.ui.note.toShortenHex
import fr.acinq.secp256k1.Hex
import java.util.concurrent.ConcurrentHashMap

class Channel(val idHex: String) {
    var creator: User? = null
    var info = ChannelCreateEvent.ChannelData(null, null, null)

    var updatedMetadataAt: Long = 0

    val notes = ConcurrentHashMap<HexKey, Note>()

    fun id() = Hex.decode(idHex)
    fun idNote() = id().toNote()
    fun idDisplayNote() = idNote().toShortenHex()

    fun toBestDisplayName(): String {
        return info.name ?: idDisplay