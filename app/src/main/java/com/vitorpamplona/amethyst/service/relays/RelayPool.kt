
package com.vitorpamplona.amethyst.service.relays

import androidx.lifecycle.LiveData
import com.vitorpamplona.amethyst.service.model.Event
import com.vitorpamplona.amethyst.service.model.EventInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * RelayPool manages the connection to multiple Relays and lets consumers deal with simple events.
 */
object RelayPool : Relay.Listener {

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var relays = listOf<Relay>()
    private var listeners = setOf<Listener>()

    fun availableRelays(): Int {
        return relays.size
    }

    fun connectedRelays(): Int {
        return relays.filter { it.isConnected() }.size
    }

    fun getRelay(url: String): Relay? {
        return relays.firstOrNull() { it.url == url }
    }

    fun loadRelays(relayList: List<Relay>) {
        if (!relayList.isNullOrEmpty()) {
            relayList.forEach { addRelay(it) }
        } else {
            Constants.convertDefaultRelays().forEach { addRelay(it) }
        }
    }

    fun unloadRelays() {
        relays.forEach { it.unregister(this) }
        relays = listOf()
    }

    fun requestAndWatch() {
        relays.forEach { it.requestAndWatch() }
    }

    fun sendFilter(subscriptionId: String) {
        relays.forEach { it.sendFilter(subscriptionId) }
    }

    fun sendFilterOnlyIfDisconnected() {
        relays.forEach { it.sendFilterOnlyIfDisconnected() }
    }

    fun send(signedEvent: EventInterface) {
        relays.forEach { it.send(signedEvent) }
    }

    fun close(subscriptionId: String) {
        relays.forEach { it.close(subscriptionId) }
    }

    fun disconnect() {
        relays.forEach { it.disconnect() }
    }

    fun addRelay(relay: Relay) {
        relay.register(this)
        relays += relay
    }

    fun removeRelay(relay: Relay) {
        relay.unregister(this)
        relays = relays.minus(relay)
    }

    fun register(listener: Listener) {
        listeners = listeners.plus(listener)
    }

    fun unregister(listener: Listener) {
        listeners = listeners.minus(listener)
    }

    interface Listener {
        fun onEvent(event: Event, subscriptionId: String, relay: Relay)

        fun onError(error: Error, subscriptionId: String, relay: Relay)

        fun onRelayStateChange(type: Relay.Type, relay: Relay, channel: String?)

        fun onSendResponse(eventId: String, success: Boolean, message: String, relay: Relay)
    }

    override fun onEvent(relay: Relay, subscriptionId: String, event: Event) {
        listeners.forEach { it.onEvent(event, subscriptionId, relay) }
    }

    override fun onError(relay: Relay, subscriptionId: String, error: Error) {
        listeners.forEach { it.onError(error, subscriptionId, relay) }
        refreshObservers()
    }

    override fun onRelayStateChange(relay: Relay, type: Relay.Type, channel: String?) {
        listeners.forEach { it.onRelayStateChange(type, relay, channel) }
        refreshObservers()
    }

    override fun onSendResponse(relay: Relay, eventId: String, success: Boolean, message: String) {
        listeners.forEach { it.onSendResponse(eventId, success, message, relay) }
    }

    // Observers line up here.
    val live: RelayPoolLiveData = RelayPoolLiveData(this)

    private fun refreshObservers() {
        scope.launch {
            live.refresh()
        }
    }
}

class RelayPoolLiveData(val relays: RelayPool) : LiveData<RelayPoolState>(RelayPoolState(relays)) {
    fun refresh() {
        postValue(RelayPoolState(relays))
    }
}

class RelayPoolState(val relays: RelayPool)