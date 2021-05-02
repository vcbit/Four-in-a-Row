package com.vitorpamplona.amethyst.ui

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.ServiceManager
import com.vitorpamplona.amethyst.service.nip19.Nip19
import com.vitorpamplona.amethyst.service.relays.Client
import com.vitorpamplona.amethyst.ui.screen.AccountScreen
import com.vitorpamplona.amethyst.ui.screen.AccountStateViewModel
import com.vitorpamplona.amethyst.ui.theme.AmethystTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nip19 = Nip19.uriToRoute(intent?.data?.toString())
        val startingPage = when (nip19?.type) {
            Nip19.Type.USER -> "User/${nip19.hex}"
            Nip19.Type.NOTE -> "Note/${nip19.hex}"
            else -> null
        }

        Coil.setImageLoader {
       