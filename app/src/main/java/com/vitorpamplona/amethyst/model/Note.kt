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

open class Note(val idHex: String)