package com.vitorpamplona.amethyst.service.relays

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class TypedFilter(
    val types: Set<FeedType>,
    val filter: JsonFilter
) {

    fun toJson(): String {
        return GsonBuilder().create().toJson(toJsonObject())
    }

    fun toJsonObject(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.add("types", typesToJson(types))
        jsonObject.add("filter", filterToJson(filter))
        return jsonObject
    }

    fun typesToJson(types: Set<FeedType>): JsonArray {
        return JsonArray().apply { types.forEach { add(it.name.lowercase()) } }
    }

    fun filterToJson(filter: JsonFilter): JsonObject {
        val jsonObject = JsonObject()
        filter.ids?.run {
            jsonObject.ad