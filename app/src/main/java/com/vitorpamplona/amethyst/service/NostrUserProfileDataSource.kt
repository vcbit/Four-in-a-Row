
package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.BadgeAwardEvent
import com.vitorpamplona.amethyst.service.model.BadgeProfilesEvent
import com.vitorpamplona.amethyst.service.model.ContactListEvent
import com.vitorpamplona.amethyst.service.model.LnZapEvent
import com.vitorpamplona.amethyst.service.model.LongTextNoteEvent
import com.vitorpamplona.amethyst.service.model.MetadataEvent
import com.vitorpamplona.amethyst.service.model.TextNoteEvent
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter

object NostrUserProfileDataSource : NostrDataSource("UserProfileFeed") {
    var user: User? = null

    fun loadUserProfile(userId: String?) {
        if (userId != null) {
            user = LocalCache.getOrCreateUser(userId)
        } else {
            user = null
        }

        resetFilters()
    }

    fun createUserInfoFilter() = user?.let {
        TypedFilter(
            types = FeedType.values().toSet(),
            filter = JsonFilter(
                kinds = listOf(MetadataEvent.kind),
                authors = listOf(it.pubkeyHex),
                limit = 1
            )
        )
    }

    fun createUserPostsFilter() = user?.let {
        TypedFilter(