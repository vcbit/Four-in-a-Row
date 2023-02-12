package com.vitorpamplona.amethyst

import com.vitorpamplona.amethyst.service.model.ATag
import com.vitorpamplona.amethyst.service.nip19.Nip19
import org.junit.Assert.assertEquals
import org.junit.Test

class NIP19ParserTest {
    @Test
    fun nAddrParser() {
        val result = Nip19.uriToRoute("nostr:naddr1qqqqygzxpsj7dqha57pjk5k37gkn6g4nzakewtmqmnwryyhd3jfwlpgxtspsgqqqw4rs3xyxus")
        assertEquals("30023:460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c:", result?.hex)
    }

    @Test
    fun nAddrParser2() {
        val result = Nip19.uriToRoute("nostr:naddr1qq8kwatfv3jj6amfwfjkwatpwfjqygxsm6lelvfda7qlg0tud9pfhduysy4vrexj65azqtdk4tr75j6xdspsgqqqw4rsg32ag8")
        assertEquals("30023:d0debf9fb12def81f43d7c69429bb784812ac1e4d2d53a202db6aac7ea4b466c:guide-wireguard", result?.hex)
    }

    @Test
    fun nAddrParse3() {
        val result = Nip19.uriToRoute("naddr1qqyrswtyv5mnjv3sqy2