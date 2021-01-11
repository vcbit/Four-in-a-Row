package com.vitorpamplona.amethyst.model

import androidx.lifecycle.LiveData
import com.vitorpamplona.amethyst.service.NostrSingleEventDataSource
import com.vitorpamplona.amethyst.service.model.*
import com.vitorpamplona.amethyst.service.relays.Relay
import com.vitorpamplona.amethyst.ui.note.toShortenHex
import fr.acinq.secp256k1.Hex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

val tagSearch = Pattern.compile("(?:\\s|\\A)\\#\\[([0-9]+)\\]")

class AddressableNote(val address: ATag) : Note(address.toTag()) {
    override fun idNote() = address.toNAddr()
    override fun idDisplayNote() = idNote().toShortenHex()
    override fun address() = address
    override fun createdAt() = (event as? LongTextNoteEvent)?.publishedAt() ?: event?.createdAt()
}

open class Note(val idHex: String) {
    // These fields are only available after the Text Note event is received.
    // They are immutable after that.
    var event: EventInterface? = null
    var author: User? = null
    var mentions: List<User>? = null
    var replyTo: List<Note>? = null

    // These fields are updated every time an event related to this note is received.
    var replies = setOf<Note>()
        private set
    var reactions = setOf<Note>()
        private set
    var boosts = setOf<Note>()
        private set
    var reports = mapOf<User, Set<Note>>()
        private set
    var zaps = mapOf<Note, Note?>()
        private set

    var relays = setOf<String>()
        private set

    var lastReactionsDownloadTime: Long? = null

    fun id() = Hex.decode(idHex)
    open fun idNote() = id().toNote()
    open fun idDisplayNote() = idNote().toShortenHex()

    fun channel(): Channel? {
        val channelHex =
            (event as? ChannelMessageEvent)?.channel()
                ?: (event as? ChannelMetadataEvent)?.channel()
                ?: (event as? ChannelCreateEvent)?.let { it.id }

        return channelHex?.let { LocalCache.checkGetOrCreateChannel(it) }
    }

    open fun address() = (event as? LongTextNoteEvent)?.address()

    open fun createdAt() = event?.createdAt()

    fun loadEvent(event: Event, author: User, mentions: List<User>, replyTo: List<Note>) {
        this.event = event
        this.author = author
        this.mentions = mentions
        this.replyTo = replyTo

        liveSet?.metadata?.invalidateData()
    }

    fun formattedDateTime(timestamp: Long): String {
        return Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("uuuu-MM-dd-HH:mm:ss"))
    }

    /**
     * This method caches signatures during each execution to avoid recalculation in longer threads
     */
    fun replyLevelSignature(cachedSignatures: MutableMap<Note, String> = mutableMapOf()): String {
        val replyTo = replyTo
        if (replyTo == null || replyTo.isEmpty()) {
            return "/" + formattedDateTime(createdAt() ?: 0) + ";"
        }

        return replyTo
            .map {
                cachedSignatures[it] ?: it.replyLevelSignature(cachedSignatures).apply { cachedSignatures.put(it, this) }
            }
            .maxBy { it.length }.removeSuffix(";") + "/" + formattedDateTime(createdAt()