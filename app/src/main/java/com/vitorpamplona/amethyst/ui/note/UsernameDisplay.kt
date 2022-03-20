package com.vitorpamplona.amethyst.ui.note

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User

@Composable
fun NoteUsernameDisplay(baseNote: Note, weight: Modifier = Modifier) {
    val noteState by baseNote.live().metadata.observeAsState()
    val note = noteState?.note ?: return

    val author = note.author

    if (author !