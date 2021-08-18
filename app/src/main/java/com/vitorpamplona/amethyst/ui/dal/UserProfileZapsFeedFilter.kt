
package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.zaps.UserZaps

object UserProfileZapsFeedFilter : FeedFilter<Pair<Note, Note>>() {
    var user: User? = null

    fun loadUserProfile(userId: String) {
        user = LocalCache.checkGetOrCreateUser(userId)
    }

    override fun feed(): List<Pair<Note, Note>> {
        return UserZaps.forProfileFeed(user?.zaps)
    }
}