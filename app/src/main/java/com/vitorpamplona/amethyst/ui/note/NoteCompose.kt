
package com.vitorpamplona.amethyst.ui.note

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow
import com.vitorpamplona.amethyst.NotificationCache
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.BadgeAwardEvent
import com.vitorpamplona.amethyst.service.model.BadgeDefinitionEvent
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.service.model.ChannelMessageEvent
import com.vitorpamplona.amethyst.service.model.ChannelMetadataEvent
import com.vitorpamplona.amethyst.service.model.LongTextNoteEvent
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.service.model.ReactionEvent
import com.vitorpamplona.amethyst.service.model.ReportEvent
import com.vitorpamplona.amethyst.service.model.RepostEvent
import com.vitorpamplona.amethyst.service.model.TextNoteEvent
import com.vitorpamplona.amethyst.ui.components.ObserveDisplayNip05Status
import com.vitorpamplona.amethyst.ui.components.ResizeImage
import com.vitorpamplona.amethyst.ui.components.RobohashAsyncImage
import com.vitorpamplona.amethyst.ui.components.RobohashAsyncImageProxy
import com.vitorpamplona.amethyst.ui.components.RobohashFallbackAsyncImage
import com.vitorpamplona.amethyst.ui.components.TranslateableRichTextViewer
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ChannelHeader
import com.vitorpamplona.amethyst.ui.theme.Following
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCompose(
    baseNote: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    isBoostedNote: Boolean = false,
    isQuotedNote: Boolean = false,
    unPackReply: Boolean = true,
    makeItShort: Boolean = false,
    addMarginTop: Boolean = true,
    parentBackgroundColor: Color? = null,
    accountViewModel: AccountViewModel,
    navController: NavController
) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return

    val noteState by baseNote.live().metadata.observeAsState()
    val note = noteState?.note

    val noteReportsState by baseNote.live().reports.observeAsState()
    val noteForReports = noteReportsState?.note ?: return

    var popupExpanded by remember { mutableStateOf(false) }
    var showHiddenNote by remember { mutableStateOf(false) }

    val context = LocalContext.current.applicationContext

    var moreActionsExpanded by remember { mutableStateOf(false) }

    val noteEvent = note?.event
    val baseChannel = note?.channel()

    if (noteEvent == null) {
        BlankNote(
            modifier.combinedClickable(
                onClick = { },
                onLongClick = { popupExpanded = true }
            ),
            isBoostedNote
        )
    } else if (!account.isAcceptable(noteForReports) && !showHiddenNote) {
        HiddenNote(
            account.getRelevantReports(noteForReports),
            account.userProfile(),
            modifier,
            isBoostedNote,
            navController,
            onClick = { showHiddenNote = true }
        )
    } else if ((noteEvent is ChannelCreateEvent || noteEvent is ChannelMetadataEvent) && baseChannel != null) {
        ChannelHeader(baseChannel = baseChannel, account = account, navController = navController)
    } else if (noteEvent is BadgeDefinitionEvent) {
        BadgeDisplay(baseNote = note)
    } else {
        var isNew by remember { mutableStateOf<Boolean>(false) }

        LaunchedEffect(key1 = routeForLastRead) {
            withContext(Dispatchers.IO) {
                routeForLastRead?.let {
                    val lastTime = NotificationCache.load(it)

                    val createdAt = note.createdAt()
                    if (createdAt != null) {
                        NotificationCache.markAsRead(it, createdAt)
                        isNew = createdAt > lastTime
                    }
                }
            }
        }

        val backgroundColor = if (isNew) {
            val newColor = MaterialTheme.colors.primary.copy(0.12f)
            if (parentBackgroundColor != null) {
                newColor.compositeOver(parentBackgroundColor)
            } else {
                newColor.compositeOver(MaterialTheme.colors.background)
            }
        } else {
            parentBackgroundColor ?: MaterialTheme.colors.background
        }

        Column(
            modifier = modifier
                .combinedClickable(
                    onClick = {
                        if (noteEvent is ChannelMessageEvent) {
                            baseChannel?.let {
                                navController.navigate("Channel/${it.idHex}")
                            }
                        } else if (noteEvent is PrivateDmEvent) {
                            navController.navigate("Room/${note.author?.pubkeyHex}") {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate("Note/${note.idHex}") {
                                launchSingleTop = true
                            }
                        }
                    },
                    onLongClick = { popupExpanded = true }
                )
                .background(backgroundColor)
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = if (!isBoostedNote) 12.dp else 0.dp,
                        end = if (!isBoostedNote) 12.dp else 0.dp,
                        top = if (addMarginTop) 10.dp else 0.dp
                    )
            ) {
                if (!isBoostedNote && !isQuotedNote) {
                    Column(Modifier.width(55.dp)) {
                        // Draws the boosted picture outside the boosted card.
                        Box(
                            modifier = Modifier
                                .width(55.dp)
                                .padding(0.dp)
                        ) {
                            NoteAuthorPicture(note, navController, account.userProfile(), 55.dp)

                            if (noteEvent is RepostEvent) {
                                note.replyTo?.lastOrNull()?.let {
                                    Box(
                                        Modifier
                                            .width(30.dp)
                                            .height(30.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        NoteAuthorPicture(
                                            it,
                                            navController,
                                            account.userProfile(),
                                            35.dp,
                                            pictureModifier = Modifier.border(2.dp, MaterialTheme.colors.background, CircleShape)
                                        )
                                    }
                                }
                            }

                            // boosted picture
                            if (noteEvent is ChannelMessageEvent && baseChannel != null) {
                                val channelState by baseChannel.live.observeAsState()
                                val channel = channelState?.channel

                                if (channel != null) {
                                    Box(
                                        Modifier
                                            .width(30.dp)
                                            .height(30.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        RobohashAsyncImageProxy(
                                            robot = channel.idHex,
                                            model = ResizeImage(channel.profilePicture(), 30.dp),
                                            contentDescription = stringResource(R.string.group_picture),
                                            modifier = Modifier
                                                .width(30.dp)
                                                .height(30.dp)
                                                .clip(shape = CircleShape)
                                                .background(MaterialTheme.colors.background)
                                                .border(
                                                    2.dp,
                                                    MaterialTheme.colors.background,
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        if (noteEvent is RepostEvent) {
                            note.replyTo?.lastOrNull()?.let {
                                RelayBadges(it)
                            }
                        } else {
                            RelayBadges(baseNote)
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(start = if (!isBoostedNote && !isQuotedNote) 10.dp else 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isQuotedNote) {
                            NoteAuthorPicture(note, navController, account.userProfile(), 25.dp)
                            Spacer(Modifier.padding(horizontal = 5.dp))
                            NoteUsernameDisplay(note, Modifier.weight(1f))
                        } else {
                            NoteUsernameDisplay(note, Modifier.weight(1f))
                        }

                        if (noteEvent is RepostEvent) {
                            Text(
                                "  ${stringResource(id = R.string.boosted)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                            )
                        }

                        Text(
                            timeAgo(note.createdAt(), context = context),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f),
                            maxLines = 1
                        )

                        IconButton(
                            modifier = Modifier.then(Modifier.size(24.dp)),
                            onClick = { moreActionsExpanded = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                            )

                            NoteDropDownMenu(baseNote, moreActionsExpanded, { moreActionsExpanded = false }, accountViewModel)
                        }
                    }

                    if (note.author != null && !makeItShort) {
                        ObserveDisplayNip05Status(note.author!!)
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    if (noteEvent is TextNoteEvent && (note.replyTo != null || note.mentions != null)) {
                        val replyingDirectlyTo = note.replyTo?.lastOrNull()
                        if (replyingDirectlyTo != null && unPackReply) {
                            NoteCompose(
                                baseNote = replyingDirectlyTo,
                                isQuotedNote = true,
                                modifier = Modifier
                                    .padding(0.dp)
                                    .fillMaxWidth()
                                    .clip(shape = RoundedCornerShape(15.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                                        RoundedCornerShape(15.dp)
                                    ),
                                unPackReply = false,
                                makeItShort = true,
                                parentBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f).compositeOver(backgroundColor),
                                accountViewModel = accountViewModel,
                                navController = navController
                            )
                        } else {
                            ReplyInformation(note.replyTo, note.mentions, account, navController)
                        }
                    } else if (noteEvent is ChannelMessageEvent && (note.replyTo != null || note.mentions != null)) {
                        val sortedMentions = note.mentions?.toSet()?.sortedBy { account.userProfile().isFollowing(it) }

                        note.channel()?.let {
                            ReplyInformationChannel(note.replyTo, sortedMentions, it, navController)
                        }
                    }

                    if (noteEvent is ReactionEvent || noteEvent is RepostEvent) {
                        note.replyTo?.lastOrNull()?.let {
                            NoteCompose(
                                it,
                                modifier = Modifier,
                                isBoostedNote = true,
                                unPackReply = false,
                                parentBackgroundColor = backgroundColor,
                                accountViewModel = accountViewModel,
                                navController = navController
                            )
                        }

                        // Reposts have trash in their contents.
                        if (noteEvent is ReactionEvent) {
                            val refactorReactionText =
                                if (noteEvent.content == "+") "❤" else noteEvent.content

                            Text(
                                text = refactorReactionText
                            )
                        }
                    } else if (noteEvent is ReportEvent) {
                        val reportType = (noteEvent.reportedPost() + noteEvent.reportedAuthor()).map {
                            when (it.reportType) {
                                ReportEvent.ReportType.EXPLICIT -> stringResource(R.string.explicit_content)
                                ReportEvent.ReportType.NUDITY -> stringResource(R.string.nudity)
                                ReportEvent.ReportType.PROFANITY -> stringResource(R.string.profanity_hateful_speech)
                                ReportEvent.ReportType.SPAM -> stringResource(R.string.spam)
                                ReportEvent.ReportType.IMPERSONATION -> stringResource(R.string.impersonation)
                                ReportEvent.ReportType.ILLEGAL -> stringResource(R.string.illegal_behavior)
                            }
                        }.toSet().joinToString(", ")

                        Text(
                            text = reportType
                        )

                        Divider(
                            modifier = Modifier.padding(top = 40.dp),
                            thickness = 0.25.dp
                        )
                    } else if (noteEvent is LongTextNoteEvent) {
                        LongFormHeader(noteEvent)

                        ReactionsRow(note, accountViewModel)

                        Divider(
                            modifier = Modifier.padding(top = 10.dp),
                            thickness = 0.25.dp
                        )
                    } else if (noteEvent is BadgeAwardEvent && !note.replyTo.isNullOrEmpty()) {
                        Text(text = stringResource(R.string.award_granted_to))

                        FlowRow(modifier = Modifier.padding(top = 5.dp)) {
                            note.mentions?.forEach {
                                UserPicture(
                                    user = it,
                                    navController = navController,
                                    userAccount = account.userProfile(),
                                    size = 35.dp
                                )
                            }
                        }

                        note.replyTo?.firstOrNull()?.let {
                            NoteCompose(
                                it,
                                modifier = Modifier,
                                isBoostedNote = false,
                                isQuotedNote = true,
                                unPackReply = false,
                                parentBackgroundColor = backgroundColor,
                                accountViewModel = accountViewModel,
                                navController = navController
                            )
                        }

                        ReactionsRow(note, accountViewModel)

                        Divider(
                            modifier = Modifier.padding(top = 10.dp),
                            thickness = 0.25.dp
                        )
                    } else if (noteEvent is PrivateDmEvent &&
                        noteEvent.recipientPubKey() != account.userProfile().pubkeyHex &&
                        note.author != account.userProfile()
                    ) {
                        val recepient = noteEvent.recipientPubKey()?.let { LocalCache.checkGetOrCreateUser(it) }

                        TranslateableRichTextViewer(
                            stringResource(
                                id = R.string.private_conversation_notification,
                                "@${note.author?.pubkeyNpub()}",
                                "@${recepient?.pubkeyNpub()}"
                            ),
                            canPreview = !makeItShort,
                            Modifier.fillMaxWidth(),
                            noteEvent.tags(),
                            backgroundColor,
                            accountViewModel,
                            navController
                        )