
package com.vitorpamplona.amethyst.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class Nip05Verifier {
    val client = OkHttpClient.Builder().build()

    fun assembleUrl(nip05address: String): String? {
        val parts = nip05address.trim().split("@")

        if (parts.size == 2) {
            return "https://${parts[1]}/.well-known/nostr.json?name=${parts[0]}"
        }

        return null
    }

    fun fetchNip05Json(nip05address: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            fetchNip05JsonSuspend(nip05address, onSuccess, onError)
        }
    }

    private suspend fun fetchNip05JsonSuspend(nip05: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val url = assembleUrl(nip05)

        if (url == null) {
            onError("Could not assemble url from Nip05: \"${nip05}\". Check the user's setup")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .header("User-Agent", "Amethyst")
                    .url(url)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {