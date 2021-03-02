package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter

object NostrChatroomDataSource : NostrDataSource("ChatroomFeed") {
    lateinit var account: Account
    var withUser: User? = null

    fun loadMessagesBetween(accountIn: Account, userId: String) {
        account = accountIn
        withUser = LocalCache.users[userId]
        resetFilters()
    }

    fun createMessagesToMeFilter(): TypedFilter? {
        val myPeer = withUser

        return if (myPeer != null) {
            TypedFilter(
                types = setOf(FeedType.PRIVATE_DMS),
                filter = JsonFilter(