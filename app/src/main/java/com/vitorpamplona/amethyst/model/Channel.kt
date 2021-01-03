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
        return info.name ?: idDisplayNote()
    }

    fun addNote(note: Note) {
        notes[note.idHex] = note
    }

    fun updateChannelInfo(creator: User, channelInfo: ChannelCreateEvent.ChannelData, updatedAt: Long) {
        this.creator = creator
        this.info = channelInfo
        this.updatedMetadataAt = updatedAt

        live.refresh()
    }

    fun profilePicture(): String? {
        if (info.picture.isNullOrBlank()) info.picture = null
        return info.picture
    }

    fun anyNameStartsWith(prefix: