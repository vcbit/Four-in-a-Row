package com.vitorpamplona.amethyst.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.service.nip19.Nip19

@Composable
fun ClickableRoute(
    nip19: Nip19.Return,
    navController: NavController
) {
    if (nip19.type == Nip19.Type.USER) {
        val userBase = LocalCache.getOrCreateUser(nip19.hex)

        val userState by userBase.live().metadata.observeAsState()
        val user = userState?.user ?: return

        val route = "User/${nip19.hex}"
        val text = user.toBestDisplayName()

        ClickableText(
            text = AnnotatedString("@$text "),
            onClick = { navController.navigate(route) },
            style = LocalTextStyle.current.copy(color = MaterialTheme.colors.primary)
        )
    } else if (nip19.type ==