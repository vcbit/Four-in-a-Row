
package com.vitorpamplona.amethyst.model

import android.util.Log
import androidx.lifecycle.LiveData
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.reflect.TypeToken
import com.vitorpamplona.amethyst.service.model.*
import com.vitorpamplona.amethyst.service.relays.Relay
import fr.acinq.secp256k1.Hex
import kotlinx.coroutines.*
import nostr.postr.toNpub
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object LocalCache {
    val metadataParser = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readerFor(UserMetadata::class.java)

    val antiSpam = AntiSpamFilter()

    val users = ConcurrentHashMap<HexKey, User>()
    val notes = ConcurrentHashMap<HexKey, Note>()
    val channels = ConcurrentHashMap<HexKey, Channel>()
    val addressables = ConcurrentHashMap<String, AddressableNote>()

    fun checkGetOrCreateUser(key: String): User? {
        if (isValidHexNpub(key)) {
            return getOrCreateUser(key)
        }
        return null
    }

    @Synchronized
    fun getOrCreateUser(key: HexKey): User {
        return users[key] ?: run {
            val answer = User(key)
            users.put(key, answer)
            answer
        }
    }

    fun checkGetOrCreateNote(key: String): Note? {
        if (ATag.isATag(key)) {
            return checkGetOrCreateAddressableNote(key)
        }
        if (isValidHexNpub(key)) {
            return getOrCreateNote(key)
        }
        return null
    }

    @Synchronized
    fun getOrCreateNote(idHex: String): Note {
        return notes[idHex] ?: run {
            val answer = Note(idHex)
            notes.put(idHex, answer)
            answer
        }
    }

    fun checkGetOrCreateChannel(key: String): Channel? {
        if (isValidHexNpub(key)) {
            return getOrCreateChannel(key)
        }
        return null
    }

    private fun isValidHexNpub(key: String): Boolean {
        return try {
            Hex.decode(key).toNpub()
            true
        } catch (e: IllegalArgumentException) {
            Log.e("LocalCache", "Invalid Key to create user: $key", e)
            false
        }
    }

    @Synchronized
    fun getOrCreateChannel(key: String): Channel {
        return channels[key] ?: run {
            val answer = Channel(key)
            channels.put(key, answer)
            answer
        }
    }

    fun checkGetOrCreateAddressableNote(key: String): AddressableNote? {
        return try {
            val addr = ATag.parse(key, null) // relay doesn't matter for the index.
            if (addr != null) {
                getOrCreateAddressableNote(addr)
            } else {
                null
            }
        } catch (e: IllegalArgumentException) {
            Log.e("LocalCache", "Invalid Key to create channel: $key", e)
            null
        }
    }

    @Synchronized
    fun getOrCreateAddressableNote(key: ATag): AddressableNote {
        // we can't use naddr here because naddr might include relay info and
        // the preferred relay should not be part of the index.
        return addressables[key.toTag()] ?: run {
            val answer = AddressableNote(key)
            answer.author = checkGetOrCreateUser(key.pubKeyHex)
            addressables.put(key.toTag(), answer)
            answer
        }
    }

    fun consume(event: MetadataEvent) {
        // new event
        val oldUser = getOrCreateUser(event.pubKey)
        if (oldUser.info == null || event.createdAt > oldUser.info!!.updatedMetadataAt) {
            val newUser = try {
                metadataParser.readValue(
                    ByteArrayInputStream(event.content.toByteArray(Charsets.UTF_8)),
                    UserMetadata::class.java
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Log.w("MT", "Content Parse Error ${e.localizedMessage} ${event.content}")
                return
            }

            oldUser.updateUserInfo(newUser, event)
            // Log.d("MT", "New User Metadata ${oldUser.pubkeyDisplayHex} ${oldUser.toBestDisplayName()}")
        } else {
            // Log.d("MT","Relay sent a previous Metadata Event ${oldUser.toBestDisplayName()} ${formattedDateTime(event.createdAt)} > ${formattedDateTime(oldUser.updatedAt)}")
        }
    }

    fun formattedDateTime(timestamp: Long): String {
        return Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("uuuu MMM d hh:mm a"))
    }

    fun consume(event: TextNoteEvent, relay: Relay? = null) {
        val note = getOrCreateNote(event.id)
        val author = getOrCreateUser(event.pubKey)
