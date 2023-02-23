package com.vitorpamplona.amethyst.service.zaps

import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.LnZapEventInterface
import com.vitorpamplona.amethyst.service.model.zaps.UserZaps
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class UserZapsTest {
    @Test
    fun nothing() {
        Assert.assertEquals(1, 1)
    }

    @Test
    fun user_without_zaps() {
        val actual = UserZaps.forProfileFeed(zaps = null)

        Assert.assertEquals(emptyList<Pair<Note, Note>>(), actual)
    }

    @Test
    fun avoid_duplicates_with_same_zap_request() {
        val zapRequest = mockk<Note>()

        val zaps: Map<Note, Note?> = mapOf(
            zapRequest to mockZapNoteWith("user-1", amount = 100),
            zapRequest to mockZapNoteWith("user-1", amount = 200)
        )

        val actual = UserZaps.forProfil