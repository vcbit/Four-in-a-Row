
package com.vitorpamplona.amethyst

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object NotificationCache {
    val lastReadByRoute = mutableMapOf<String, Long>()

    fun markAsRead(route: String, timestampInSecs: Long) {
        val lastTime = lastReadByRoute[route]
        if (lastTime == null || timestampInSecs > lastTime) {
            lastReadByRoute.put(route, timestampInSecs)

            val scope = CoroutineScope(Job() + Dispatchers.IO)
            scope.launch {
                LocalPreferences.saveLastRead(route, timestampInSecs)
                live.invalidateData()
            }
        }
    }

    fun load(route: String): Long {
        var lastTime = lastReadByRoute[route]
        if (lastTime == null) {
            lastTime = LocalPreferences.loadLastRead(route)
            lastReadByRoute[route] = lastTime
        }
        return lastTime
    }

    // Observers line up here.
    val live: NotificationLiveData = NotificationLiveData(this)
}

class NotificationLiveData(val cache: NotificationCache) : LiveData<NotificationState>(NotificationState(cache)) {
    // Refreshes observers in batches.
    var handlerWaiting = AtomicBoolean()

    fun invalidateData() {
        if (!hasActiveObservers()) return
        if (handlerWaiting.getAndSet(true)) return

        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            try {
                delay(100)
                refresh()
            } finally {
                withContext(NonCancellable) {
                    handlerWaiting.set(false)
                }
            }
        }
    }

    fun refresh() {
        postValue(NotificationState(cache))
    }
}

class NotificationState(val cache: NotificationCache)