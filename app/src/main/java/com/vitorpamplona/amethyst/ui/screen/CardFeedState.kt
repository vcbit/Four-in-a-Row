package com.vitorpamplona.amethyst.ui.screen

import androidx.compose.runtime.MutableState
import com.vitorpamplona.amethyst.model.Note

abstract class Card() {
    abstract fun createdAt(): Long
    abstract fun id(): String
}

class BadgeCard(val note: Note) : Card() {
    override fun createdAt(): Long {
        return note.createdAt() ?: 0
    }

    override fun id() = note.idHex
}

class NoteCard(val note: Note) : Card() {
    override fun createdAt(): Long {
        return note.createdAt() ?: 0
    }

    override fun id() 