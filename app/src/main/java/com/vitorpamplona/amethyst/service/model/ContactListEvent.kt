package com.vitorpamplona.amethyst.service.model

import android.util.Log
import com.google.gson.reflect.TypeToken
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import nostr.postr.Utils
import java.util.Date

data class Contact(val pubKeyHex: String, val relayUri: String?)

class ContactListEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {
    fun follows() = try {
        tags.filter { it[0] == "p" }.map { Contact(it[1], it.getOrNull(2)) }
    } catch (e: Exception) {
        Log.e("ContactListEvent", "can't parse tags as follows: $tags", e)
        null
    }

    fun relayUse() = try {
        if (content.isNotEmpty()) {
            gson.fromJson(content, object : TypeToken<Map<String, ReadWrite>>() {}.type)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("ContactListEvent", "can't parse content as relay lists: $tags", e)
        null
    }

    companion object {
        const val kind = 3

        fun create(follows: List<Contact>, relayUse: Map<String, ReadWrite>?, priv