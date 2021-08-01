package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.Note

object ChatroomListKnownFeedFilter : FeedFilter<Note>() {
    lateinit var account: Account

    // returns the last Note of each user.
    override fun feed(): List<Note> {
        val me = account.userProfile()

        val privateChatrooms = account.userPro