package com.vitorpamplona.amethyst.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vitorpamplona.amethyst.NotificationCache
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.service.model.ChannelMetadataEvent
import com.vitorpamplona.amethyst.ui.components.ResizeImage
import com.vitorpamplona.amethyst.ui.components.RobohashAsyncImageProxy
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChatroomCompose(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    navController: NavController
) {
    val noteState by baseNote.live().metadata.observeAsState()
    val note = noteState?.note

    val notificationCacheState = NotificationCache.live.observeAsState()
    val notificationCache = notificationCacheState.value ?: return

    if (note?.event == null) {
        BlankNote(Modifier)
    } else if (note.channel() != null) {
        val authorState by note.author!!.live().metadata.observeAsState()
        val author = authorState?.user

        val channelState by note.channel()!!.live.observeAsState()
        val channel = channelState?.channel

        val noteEvent = note.event

        val description = if (noteEvent is ChannelCreateEvent) {
            stringResource(R.string.channel_created)
        } else if (noteEvent is ChannelMetadataEvent) {
            "${stringResource(R.string.channel_information_changed_to)} "
        } else {
            noteEvent?.content()
        }
        channel?.let { chan ->
            var hasNewMessages by remember { mutableStateOf<Boolean>(false) }

            LaunchedEffect(key1 = notificationCache, key2 = note) {
                withContext(Dispatchers.IO) {
                    note.createdAt()?.let { timestamp -