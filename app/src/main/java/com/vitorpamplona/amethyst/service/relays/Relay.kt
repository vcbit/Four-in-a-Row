
package com.vitorpamplona.amethyst.service.relays

import android.util.Log
import com.google.gson.JsonElement
import com.vitorpamplona.amethyst.service.model.Event
import com.vitorpamplona.amethyst.service.model.EventInterface
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Date