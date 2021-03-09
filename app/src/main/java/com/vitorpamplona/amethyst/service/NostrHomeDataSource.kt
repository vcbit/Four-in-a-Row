
package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.UserState
import com.vitorpamplona.amethyst.service.model.LongTextNoteEvent
import com.vitorpamplona.amethyst.service.model.TextNoteEvent
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object NostrHomeDataSource : NostrDataSource("HomeFeed") {
    lateinit var account: Account

    private val cacheListener: (UserState) -> Unit = {
        invalidateFilters()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun start() {
        if (this::account.isInitialized) {
            GlobalScope.launch(Dispatchers.Main) {
                account.userProfile().live().follows.observeForever(cacheListener)
            }
        }
        super.start()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun stop() {
        super.stop()
        if (this::account.isInitialized) {
            GlobalScope.launch(Dispatchers.Main) {
                account.userProfile().live().follows.removeObserver(cacheListener)
            }
        }
    }

    fun createFollowAccountsFilter(): TypedFilter {
        val follows = account.userProfile().follows

        val followKeys = follows.map {
            it.pubkeyHex.substring(0, 6)
        }

        val followSet = followKeys.plus(account.userProfile().pubkeyHex.substring(0, 6))

        return TypedFilter(
            types = setOf(FeedType.FOLLOWS),
            filter = JsonFilter(
                kinds = listOf(TextNoteEvent.kind, LongTextNoteEvent.kind),
                authors = followSet,
                limit = 400
            )
        )
    }

    val followAccountChannel = requestNewChannel()

    override fun updateChannelFilters() {
        followAccountChannel.typedFilters = listOf(createFollowAccountsFilter()).ifEmpty { null }
    }
}