package com.vitorpamplona.amethyst

import com.vitorpamplona.amethyst.service.model.ATag
import com.vitorpamplona.amethyst.service.nip19.Nip19
import org.junit.Assert.assertEquals
import org.junit.Test

class NIP19ParserTest {
    @Test
    fun nAddrParser() {
        val result = Nip19.uriToRoute("nostr:naddr1qqqqygzxpsj7dqha57pjk5k37gkn6g4nzakewtmqmnwryyhd3jfwlpgxtspsgqqqw4rs3xyxus")
        assertEquals("30023:460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef8