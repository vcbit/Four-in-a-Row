package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User

object ChatroomFeedFilter : FeedFilter<Note>() {
    var account: Account? = null
    var withUser: User? = null

    fun loadMessagesBetween(accoun