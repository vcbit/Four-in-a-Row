
package com.vitorpamplona.amethyst.service.model

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import nostr.postr.Utils
import nostr.postr.toHex
import java.lang.reflect.Type
import java.security.MessageDigest
import java.util.*

open class Event(
    val id: HexKey,
    @SerializedName("pubkey") val pubKey: HexKey,
    @SerializedName("created_at") val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: HexKey
) : EventInterface {
    override fun id(): HexKey = id

    override fun pubKey(): HexKey = pubKey

    override fun createdAt(): Long = createdAt

    override fun kind(): Int = kind

    override fun tags(): List<List<String>> = tags

    override fun content(): String = content

    override fun sig(): HexKey = sig

    override fun toJson(): String = gson.toJson(this)

    /**
     * Checks if the ID is correct and then if the pubKey's secret key signed the event.
     */
    override fun checkSignature() {
        if (!id.contentEquals(generateId())) {
            throw Exception(
                """|Unexpected ID.
                   |  Event: ${toJson()}
                   |  Actual ID: $id
                   |  Generated: ${generateId()}
                """.trimIndent()
            )
        }
        if (!secp256k1.verifySchnorr(Hex.decode(sig), Hex.decode(id), Hex.decode(pubKey))) {
            throw Exception("""Bad signature!""")
        }
    }

    override fun hasValidSignature(): Boolean {
        if (!id.contentEquals(generateId())) {
            return false
        }

        return secp256k1.verifySchnorr(Hex.decode(sig), Hex.decode(id), Hex.decode(pubKey))
    }

    private fun generateId(): String {
        val rawEvent = listOf(0, pubKey, createdAt, kind, tags, content)

        // GSON decided to hardcode these replacements.
        // They break Nostr's hash check.
        // These lines revert their code.
        // https://github.com/google/gson/issues/2295
        val rawEventJson = gson.toJson(rawEvent)
            .replace("\\u2028", "\u2028")
            .replace("\\u2029", "\u2029")

        return sha256.digest(rawEventJson.toByteArray()).toHexKey()
    }

    private class EventDeserializer : JsonDeserializer<Event> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Event {
            val jsonObject = json.asJsonObject
            return Event(
                id = jsonObject.get("id").asString,
                pubKey = jsonObject.get("pubkey").asString,
                createdAt = jsonObject.get("created_at").asLong,
                kind = jsonObject.get("kind").asInt,
                tags = jsonObject.get("tags").asJsonArray.map {
                    it.asJsonArray.mapNotNull { s -> if (s.isJsonNull) null else s.asString }
                },
                content = jsonObject.get("content").asString,
                sig = jsonObject.get("sig").asString
            )
        }
    }

    private class EventSerializer : JsonSerializer<Event> {
        override fun serialize(
            src: Event,
            typeOfSrc: Type?,