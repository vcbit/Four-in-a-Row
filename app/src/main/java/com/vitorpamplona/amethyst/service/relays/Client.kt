
package com.vitorpamplona.amethyst.service.relays

import com.vitorpamplona.amethyst.service.model.Event
import com.vitorpamplona.amethyst.service.model.EventInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The Nostr Client manages multiple personae the user may switch between. Events are received and
 * published through multiple relays.
 * Events are stored with their respective persona.
 */
object Client : RelayPool.Listener {
    /**
     * Lenient mode:
     *
     * true: For maximum compatibility. If you want to play ball with sloppy counterparts, use
     *       this.
     * false: For developers who want to make protocol compliant counterparts. If your software
     *        produces events that fail to deserialize in strict mode, you should probably fix
     *        something.
     **/
    var lenient: Boolean = false
    private var listeners = setOf<Listener>()
    private var relays = Constants.convertDefaultRelays()
    private var subscriptions = mapOf<String, List<TypedFilter>>()

    @Synchronized
    fun connect(relays: Array<Relay>) {
        RelayPool.register(this)
        RelayPool.unloadRelays()
        RelayPool.loadRelays(relays.toList())
        this.relays = relays
    }

    fun isSameRelaySetConfig(newRelayConfig: Array<Relay>): Boolean {
        if (relays.size != newRelayConfig.size) return false

        relays.forEach { oldRelayInfo ->
            val newRelayInfo = newRelayConfig.find { it.url == oldRelayInfo.url } ?: return false

            if (!oldRelayInfo.isSameRelayConfig(newRelayInfo)) return false
        }

        return true
    }

    fun sendFilter(
        subscriptionId: String = UUID.randomUUID().toString().substring(0..10),
        filters: List<TypedFilter> = listOf()
    ) {
        subscriptions = subscriptions + Pair(subscriptionId, filters)
        RelayPool.sendFilter(subscriptionId)
    }

    fun sendFilterOnlyIfDisconnected(
        subscriptionId: String = UUID.randomUUID().toString().substring(0..10),
        filters: List<TypedFilter> = listOf()
    ) {
        subscriptions = subscriptions + Pair(subscriptionId, filters)
        RelayPool.sendFilterOnlyIfDisconnected()
    }

    fun send(signedEvent: EventInterface) {
        RelayPool.send(signedEvent)
    }

    fun close(subscriptionId: String) {
        RelayPool.close(subscriptionId)
        subscriptions = subscriptions.minus(subscriptionId)
    }

    fun disconnect() {
        RelayPool.unregister(this)
        RelayPool.disconnect()
        RelayPool.unloadRelays()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onEvent(event: Event, subscriptionId: String, relay: Relay) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onEvent(event, subscriptionId, relay) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onError(error: Error, subscriptionId: String, relay: Relay) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onError(error, subscriptionId, relay) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onRelayStateChange(type: Relay.Type, relay: Relay, channel: String?) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onRelayStateChange(type, relay, channel) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onSendResponse(eventId: String, success: Boolean, message: String, relay: Relay) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onSendResponse(eventId, success, message, relay) }
        }
    }

    fun subscribe(listener: Listener) {
        listeners = listeners.plus(listener)
    }

    fun unsubscribe(listener: Listener) {
        listeners = listeners.minus(listener)
    }

    fun allSubscriptions(): List<String> {
        return subscriptions.keys.toList()
    }

    fun getSubscriptionFilters(subId: String): List<TypedFilter> {
        return subscriptions[subId] ?: emptyList()
    }

    abstract class Listener {
        /**
         * A new message was received
         */
        open fun onEvent(event: Event, subscriptionId: String, relay: Relay) = Unit

        /**
         * A new or repeat message was received
         */
        open fun onError(error: Error, subscriptionId: String, relay: Relay) = Unit

        /**
         * Connected to or disconnected from a relay
         */
        open fun onRelayStateChange(type: Relay.Type, relay: Relay, channel: String?) = Unit

        /**
         * When an relay saves or rejects a new event.
         */
        open fun onSendResponse(eventId: String, success: Boolean, message: String, relay: Relay) = Unit
    }
}