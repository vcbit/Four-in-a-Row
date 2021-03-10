package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.decodePublicKey
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.service.model.ChannelMessageEvent
import com.vitorpamplona.amethyst.service.model.ChannelMetadataEvent
import com.vitorpamplona.amethyst.service.model.LongTextNoteEvent
import com.vitorpamplona.amethyst.service.model.MetadataEvent
import com.vitorpamplona.amethyst.service.model.TextNoteEvent
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter
import nostr.postr.bechToBytes
import nostr.postr.toHex

object NostrSearchEventOrUserDataSource : NostrDataSource("SingleEventFeed") {
    private var searchString: String? = null

    private fun createAnythingWithIDFilter(): List<TypedFilter>? {
        val mySearchString = searchString
        if (mySearchString == null) {
            return null
        }

        val hexToWatch = try {
            if (mySearchString.startsWith("npub") || mySearchString.startsWith("nsec")) {
                decodePublicKey(mySearchString).toHex()
            } else if (mySearchString.startsWith("note")) {
                mySearchString.bechToBytes().toHex()
            } else {
                mySearchString
            }
        } catch (e: Exception) {
            // Usually when people add an incomplete npub or note.
            null
        }

        if (hexToWatch == null) {
            return null
        }

        // downloads all the reactions to a given event.
        return listOf(
            TypedFilter(
                types = FeedType.values().toSet(),
                filter = JsonFilter(
                    ids = listOfNotNull(hexToWatch)
                )
            ),
            TypedFilter(
                types = FeedType.values().toSet(),
                filter = JsonFilter(
                    k