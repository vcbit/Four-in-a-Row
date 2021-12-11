package com.vitorpamplona.amethyst.ui.note

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.ui.actions.CloseButton
import com.vitorpamplona.amethyst.ui.actions.NewPostView
import com.vitorpamplona.amethyst.ui.actions.SaveButton
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.BitcoinOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReactionsRow(baseNote: Note, accountViewModel: AccountViewModel) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return

    var wantsToReplyTo by remember {
        mutableStateOf<Note?>(null)
    }

    var wantsToQuote by remember {
        mutableStateOf<Note?>(null)
    }

    if (wantsToReplyTo != null) {
        NewPostView({ wantsToReplyTo = null }, wantsToReplyTo, null, account)
    }

    if (wantsToQuote != null) {
        NewPostView({ wantsToQuote = null }, null, wantsToQuote, account)
    }

    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ReplyReaction(baseNote, accountViewModel, Modifier.weight(1f)) {
            wantsToReplyTo = baseNote
        }

        BoostReaction(baseNote, accountViewModel, Modifier.weight(1f)) {
            wantsToQuote = baseNote
        }

        LikeReaction(baseNote, accountViewModel, Modifier.weight(1f))

        ZapReaction(baseNote, accountViewModel, Modifier.weight(1f))

        ViewCountReaction(baseNote, Modifier.weight(1f))
    }
}

@Composable
fun ReplyReaction(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    textModifier: Modifier = Modifier,
    showCounter: Boolean = true,
    onPress: () -> Unit
) {
    val repliesState by baseNote.live().replies.observeAsState()
    val replies = repliesState?.note?.replies ?: emptySet()

    val grayTint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    IconButton(
        modifier = Modifier.then(Modifier.size(20.dp)),
        onClick = {
            if (accountViewModel.isWriteable()) {
                onPress()
            } else {
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.login_with_a_private_key_to_be_able_to_reply),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_comment),
            null,
            modifier = Modifier.size(15.dp),
            tint = grayTint
        )
    }

    if (showCounter) {
        Text(
            " ${showCount(replies.size)}",
            fontSize = 14.sp,
            color = grayTint,
            modifier = textModifier
        )
    }
}

@Composable
private fun BoostReaction(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    textModifier: Modifier = Modifier,
    onQuotePress: () -> Unit
) {
    val boostsState by baseNote.live().boosts.observeAsState()
    val boostedNote = boostsState?.note

    val grayTint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
    val conte