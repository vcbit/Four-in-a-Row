
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

        if (relay != null) {
            author.addRelayBeingUsed(relay, event.createdAt)
            note.addRelay(relay)
        }

        // Already processed this event.
        if (note.event != null) return

        if (antiSpam.isSpam(event)) {
            relay?.let {
                it.spamCounter++
            }
            return
        }

        val mentions = event.mentions().mapNotNull { checkGetOrCreateUser(it) }
        val replyTo = tagsWithoutCitations(event).mapNotNull { checkGetOrCreateNote(it) }

        note.loadEvent(event, author, mentions, replyTo)

        // Log.d("TN", "New Note (${notes.size},${users.size}) ${note.author?.toBestDisplayName()} ${note.event?.content()?.take(100)} ${formattedDateTime(event.createdAt)}")

        // Prepares user's profile view.
        author.addNote(note)

        // Adds notifications to users.
        mentions.forEach {
            it.addTaggedPost(note)
        }
        replyTo.forEach {
            it.author?.addTaggedPost(note)
        }

        // Counts the replies
        replyTo.forEach {
            it.addReply(note)
        }

        refreshObservers()
    }

    fun consume(event: LongTextNoteEvent, relay: Relay?) {
        val note = getOrCreateAddressableNote(event.address())
        val author = getOrCreateUser(event.pubKey)

        if (relay != null) {
            author.addRelayBeingUsed(relay, event.createdAt)
            note.addRelay(relay)
        }

        // Already processed this event.
        if (note.event?.id() == event.id()) return

        if (antiSpam.isSpam(event)) {
            relay?.let {
                it.spamCounter++
            }
            return
        }

        val mentions = event.mentions().mapNotNull { checkGetOrCreateUser(it) }
        val replyTo = tagsWithoutCitations(event).mapNotNull { checkGetOrCreateNote(it) }

        if (event.createdAt > (note.createdAt() ?: 0)) {
            note.loadEvent(event, author, mentions, replyTo)

            author.addNote(note)

            // Adds notifications to users.
            mentions.forEach {
                it.addTaggedPost(note)
            }
            replyTo.forEach {
                it.author?.addTaggedPost(note)
            }

            refreshObservers()
        }
    }

    fun consume(event: BadgeDefinitionEvent) {
        val note = getOrCreateAddressableNote(event.address())
        val author = getOrCreateUser(event.pubKey)

        // Already processed this event.
        if (note.event?.id() == event.id()) return

        if (event.createdAt > (note.createdAt() ?: 0)) {
            note.loadEvent(event, author, emptyList<User>(), emptyList<Note>())

            refreshObservers()
        }
    }

    fun consume(event: BadgeProfilesEvent) {
        val note = getOrCreateAddressableNote(event.address())
        val author = getOrCreateUser(event.pubKey)

        // Already processed this event.
        if (note.event?.id() == event.id()) return

        val replyTo = event.badgeAwardEvents().mapNotNull { checkGetOrCreateNote(it) } +
            event.badgeAwardDefinitions().mapNotNull { getOrCreateAddressableNote(it) }

        if (event.createdAt > (note.createdAt() ?: 0)) {
            note.loadEvent(event, author, emptyList(), replyTo)

            author.updateAcceptedBadges(note)

            refreshObservers()
        }
    }

    fun consume(event: BadgeAwardEvent) {
        val note = getOrCreateNote(event.id)

        // Already processed this event.
        if (note.event != null) return

        // Log.d("TN", "New Boost (${notes.size},${users.size}) ${note.author?.toBestDisplayName()} ${formattedDateTime(event.createdAt)}")

        val author = getOrCreateUser(event.pubKey)
        val awardees = event.awardees().mapNotNull { checkGetOrCreateUser(it) }
        val awardDefinition = event.awardDefinition().map { getOrCreateAddressableNote(it) }

        note.loadEvent(event, author, awardees, awardDefinition)

        // Adds notifications to users.
        awardees.forEach {
            it.addTaggedPost(note)
        }

        // Replies of an Badge Definition are Award Events
        awardDefinition.forEach {
            it.addReply(note)
        }

        refreshObservers()
    }

    private fun findCitations(event: Event): Set<String> {
        var citations = mutableSetOf<String>()
        // Removes citations from replies:
        val matcher = tagSearch.matcher(event.content)
        while (matcher.find()) {
            try {
                val tag = matcher.group(1)?.let { event.tags[it.toInt()] }
                if (tag != null && tag[0] == "e") {
                    citations.add(tag[1])
                }
                if (tag != null && tag[0] == "a") {
                    citations.add(tag[1])
                }
            } catch (e: Exception) {
            }
        }
        return citations
    }

    private fun tagsWithoutCitations(event: TextNoteEvent): List<String> {
        val repliesTo = event.replyTos()
        val tagAddresses = event.taggedAddresses().map { it.toTag() }
        if (repliesTo.isEmpty() && tagAddresses.isEmpty()) return emptyList()

        val citations = findCitations(event)

        return if (citations.isEmpty()) {
            repliesTo + tagAddresses
        } else {
            repliesTo.filter { it !in citations }
        }
    }

    private fun tagsWithoutCitations(event: LongTextNoteEvent): List<String> {
        val repliesTo = event.replyTos()
        val tagAddresses = event.taggedAddresses().map { it.toTag() }
        if (repliesTo.isEmpty() && tagAddresses.isEmpty()) return emptyList()

        val citations = findCitations(event)

        return if (citations.isEmpty()) {
            repliesTo + tagAddresses
        } else {
            repliesTo.filter { it !in citations }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun consume(event: RecommendRelayEvent) {
//        // Log.d("RR", event.toJson())
    }

    fun consume(event: ContactListEvent) {
        val user = getOrCreateUser(event.pubKey)
        val follows = event.follows()

        if (event.createdAt > user.updatedFollowsAt && !follows.isNullOrEmpty()) {
            // Saves relay list only if it's a user that is currently been seen
            user.latestContactList = event

            user.updateFollows(
                follows.map {
                    try {
                        val pubKey = decodePublicKey(it.pubKeyHex)
                        getOrCreateUser(pubKey.toHexKey())
                    } catch (e: Exception) {
                        Log.w("ContactList Parser", "Ignoring: Could not parse Hex key: ${it.pubKeyHex} in ${event.toJson()}")
                        // e.printStackTrace()
                        null
                    }
                }.filterNotNull().toSet(),
                event.createdAt
            )

            // Saves relay list only if it's a user that is currently been seen
            try {
                if (event.content.isNotEmpty()) {
                    val relays: Map<String, ContactListEvent.ReadWrite> =
                        Event.gson.fromJson(
                            event.content,
                            object : TypeToken<Map<String, ContactListEvent.ReadWrite>>() {}.type
                        )

                    user.updateRelays(relays)
                }
            } catch (e: Exception) {
                Log.w("Relay List Parser", "Relay import issue ${e.message}", e)
                e.printStackTrace()
            }

            // Log.d("CL", "AAA ${user.toBestDisplayName()} ${follows.size}")
        }
    }

    fun consume(event: PrivateDmEvent, relay: Relay?) {
        val note = getOrCreateNote(event.id)
        val author = getOrCreateUser(event.pubKey)

        if (relay != null) {
            author.addRelayBeingUsed(relay, event.createdAt)
            note.addRelay(relay)
        }

        // Already processed this event.
        if (note.event != null) return

        val recipient = event.recipientPubKey()?.let { getOrCreateUser(it) }

        // Log.d("PM", "${author.toBestDisplayName()} to ${recipient?.toBestDisplayName()}")

        val repliesTo = event.tags.filter { it.firstOrNull() == "e" }.mapNotNull { it.getOrNull(1) }.mapNotNull { checkGetOrCreateNote(it) }
        val mentions = event.tags.filter { it.firstOrNull() == "p" }.mapNotNull { it.getOrNull(1) }.mapNotNull { checkGetOrCreateUser(it) }

        note.loadEvent(event, author, mentions, repliesTo)

        if (recipient != null) {
            author.addMessage(recipient, note)
            recipient.addMessage(author, note)
        }

        refreshObservers()
    }

    fun consume(event: DeletionEvent) {
        var deletedAtLeastOne = false

        event.deleteEvents().mapNotNull { notes[it] }.forEach { deleteNote ->
            // must be the same author
            if (deleteNote.author?.pubkeyHex == event.pubKey) {
                deleteNote.author?.removeNote(deleteNote)

                // reverts the add
                deleteNote.mentions?.forEach { user ->
                    user.removeTaggedPost(deleteNote)
                    user.removeReport(deleteNote)
                }

                deleteNote.replyTo?.forEach { replyingNote ->
                    replyingNote.author?.removeTaggedPost(deleteNote)
                }

                // Counts the replies
                deleteNote.replyTo?.forEach { masterNote ->
                    masterNote.removeReply(deleteNote)
                    masterNote.removeBoost(deleteNote)
                    masterNote.removeReaction(deleteNote)
                    masterNote.removeZap(deleteNote)
                    masterNote.removeReport(deleteNote)
                }

                notes.remove(deleteNote.idHex)

                deletedAtLeastOne = true
            }
        }

        if (deletedAtLeastOne) {
            live.invalidateData()
        }
    }

    fun consume(event: RepostEvent) {
        val note = getOrCreateNote(event.id)

        // Already processed this event.
        if (note.event != null) return

        // Log.d("TN", "New Boost (${notes.size},${users.size}) ${note.author?.toBestDisplayName()} ${formattedDateTime(event.createdAt)}")

        val author = getOrCreateUser(event.pubKey)
        val mentions = event.originalAuthor().mapNotNull { checkGetOrCreateUser(it) }
        val repliesTo = event.boostedPost().mapNotNull { checkGetOrCreateNote(it) } +
            event.taggedAddresses().mapNotNull { getOrCreateAddressableNote(it) }

        note.loadEvent(event, author, mentions, repliesTo)

        // Prepares user's profile view.
        author.addNote(note)

        // Adds notifications to users.
        mentions.forEach {
            it.addTaggedPost(note)
        }
        repliesTo.forEach {
            it.author?.addTaggedPost(note)
        }

        // Counts the replies
        repliesTo.forEach {
            it.addBoost(note)
        }

        refreshObservers()
    }

    fun consume(event: ReactionEvent) {
        val note = getOrCreateNote(event.id)

        // Already processed this event.
        if (note.event != null) return

        val author = getOrCreateUser(event.pubKey)
        val mentions = event.originalAuthor().mapNotNull { checkGetOrCreateUser(it) }
        val repliesTo = event.originalPost().mapNotNull { checkGetOrCreateNote(it) } +
            event.taggedAddresses().mapNotNull { getOrCreateAddressableNote(it) }

        note.loadEvent(event, author, mentions, repliesTo)

        // Log.d("RE", "New Reaction ${event.content} (${notes.size},${users.size}) ${note.author?.toBestDisplayName()} ${formattedDateTime(event.createdAt)}")

        // Adds notifications to users.
        mentions.forEach {
            it.addTaggedPost(note)
        }
        repliesTo.forEach {
            it.author?.addTaggedPost(note)
        }

        if (
            event.content == "" ||
            event.content == "+" ||
            event.content == "\u2764\uFE0F" || // red heart
            event.content == "\uD83E\uDD19" || // call me hand
            event.content == "\uD83D\uDC4D" // thumbs up
        ) {
            // Counts the replies
            repliesTo.forEach {
                it.addReaction(note)
            }
        }

        if (event.content == "!" || // nostr_console hide.
            event.content == "\u26A0\uFE0F" // Warning sign
        ) {
            // Counts the replies
            repliesTo.forEach {
                it.addReport(note)
            }
        }
    }

    fun consume(event: ReportEvent, relay: Relay?) {
        val note = getOrCreateNote(event.id)
        val author = getOrCreateUser(event.pubKey)

        if (relay != null) {
            author.addRelayBeingUsed(relay, event.createdAt)
            note.addRelay(relay)
        }

        // Already processed this event.
        if (note.event != null) return

        val mentions = event.reportedAuthor().mapNotNull { checkGetOrCreateUser(it.key) }
        val repliesTo = event.reportedPost().mapNotNull { checkGetOrCreateNote(it.key) } +
            event.taggedAddresses().mapNotNull { getOrCreateAddressableNote(it) }

        note.loadEvent(event, author, mentions, repliesTo)

        // Log.d("RP", "New Report ${event.content} by ${note.author?.toBestDisplayName()} ${formattedDateTime(event.createdAt)}")
        // Adds notifications to users.
        if (repliesTo.isEmpty()) {
            mentions.forEach {
                it.addReport(note)
            }
        }
        repliesTo.forEach {
            it.addReport(note)
        }
    }

    fun consume(event: ChannelCreateEvent) {
        // Log.d("MT", "New Event ${event.content} ${event.id.toHex()}")
        val oldChannel = getOrCreateChannel(event.id)
        val author = getOrCreateUser(event.pubKey)
        if (event.createdAt <= oldChannel.updatedMetadataAt) {
            return // older data, does nothing
        }
        if (oldChannel.creator == null || oldChannel.creator == author) {
            oldChannel.updateChannelInfo(author, event.channelInfo(), event.createdAt)

            val note = getOrCreateNote(event.id)
            oldChannel.addNote(note)
            note.loadEvent(event, author, emptyList(), emptyList())

            refreshObservers()
        }
    }

    fun consume(event: ChannelMetadataEvent) {
        val channelId = event.channel()
        // Log.d("MT", "New User ${users.size} ${event.contactMetaData.name}")
        if (channelId.isNullOrBlank()) return

        // new event
        val oldChannel = checkGetOrCreateChannel(channelId) ?: return
        val author = getOrCreateUser(event.pubKey)
        if (event.createdAt > oldChannel.updatedMetadataAt) {
            if (oldChannel.creator == null || oldChannel.creator == author) {
                oldChannel.updateChannelInfo(author, event.channelInfo(), event.createdAt)

                val note = getOrCreateNote(event.id)
                oldChannel.addNote(note)
                note.loadEvent(event, author, emptyList(), emptyList())

                refreshObservers()
            }
        } else {
            // Log.d("MT","Relay sent a previous Metadata Event ${oldUser.toBestDisplayName()} ${formattedDateTime(event.createdAt)} > ${formattedDateTime(oldUser.updatedAt)}")
        }
    }

    fun consume(event: ChannelMessageEvent, relay: Relay?) {
        val channelId = event.channel()

        if (channelId.isNullOrBlank()) return

        val channel = checkGetOrCreateChannel(channelId) ?: return

        val note = getOrCreateNote(event.id)
        channel.addNote(note)

        val author = getOrCreateUser(event.pubKey)

        if (relay != null) {
            author.addRelayBeingUsed(relay, event.createdAt)
            note.addRelay(relay)
        }

        // Already processed this event.
        if (note.event != null) return

        if (antiSpam.isSpam(event)) {
            relay?.let {
                it.spamCounter++
            }
            return
        }

        val mentions = event.mentions().mapNotNull { checkGetOrCreateUser(it) }
        val replyTo = event.replyTos()
            .mapNotNull { checkGetOrCreateNote(it) }
            .filter { it.event !is ChannelCreateEvent }

        note.loadEvent(event, author, mentions, replyTo)

        // Log.d("CM", "New Note (${notes.size},${users.size}) ${note.author?.toBestDisplayName()} ${note.event?.content()} ${formattedDateTime(event.createdAt)}")

        // Adds notifications to users.
        mentions.forEach {
            it.addTaggedPost(note)