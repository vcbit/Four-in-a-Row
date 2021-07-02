package com.vitorpamplona.amethyst.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.service.lnurl.LnInvoiceUtil
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel

const val SHORT_TEXT_LENGTH = 350

@Composable
fun ExpandableRichTextViewer(
    content: String,
    canPreview: Boolean,
    modifier: Modifier = Modifier,
    tags: List<List<String>>?,
    backgroundColor: Color,
    accountViewModel: AccountViewModel,
    navController: NavController
) {
    var showFullText by remember { mutableStateOf(false) }

    val text = if (showFullText) {
        content
    } else {
        val (lnStart, lnEnd) = LnInvoiceUtil.locateInvoice(content)
        if (lnStart < SHORT_TEXT_LENGTH && lnEnd > 0) {
            content.take(lnEnd)
        } else {
            content.take(SHORT_TEXT_LENGTH)
        }
    }

    Box(contentAlignment = Alignment.BottomCenter) {
        // CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        RichTextViewer(
            text,
            canPreview,
            modifier,
            tags,
            backgroundColor,
            accountViewModel,
            navController
        )
        // }

        if (content.length > 350 && !showFullText) {
           