package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.service.model.ChannelMetadataEvent
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter

object NostrSingleChannelDataSource : NostrDataSource("SingleChannelFeed") {
    private var channelsToWatch = setOf<String>()

    private fun createRepliesAndReactionsFilter(): TypedFilter? {
        val reactionsToWatch = channelsToWatch.map { it }

        if (reactionsToWatch.isEmpty()) {
            return null
        }

        // 