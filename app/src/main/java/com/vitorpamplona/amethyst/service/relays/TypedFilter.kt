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