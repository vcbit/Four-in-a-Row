package com.vitorpamplona.amethyst.model

import android.util.Log
import android.util.LruCache
import androidx.lifecycle.LiveData
import com.vitorpamplona.amethyst.service.model.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class Spammer(val pubkeyHex: HexKey, var duplicatedMessages: Set<HexKey>)

class AntiSpamFilter {
    val recentMessages = LruCache<Int, String>(1000)
    val spamMessages = LruCache<Int, Spammer>(1000)

    @Synchro