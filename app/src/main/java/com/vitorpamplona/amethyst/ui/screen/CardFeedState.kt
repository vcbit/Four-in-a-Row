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

    override fun id() = note.idHex
}

class LikeSetCard(val note: Note, val likeEvents: List<Note>) : Card() {
    val createdAt = likeEvents.maxOf { it.createdAt() ?: 0 }
    override fun createdAt(): Long {
        return createdAt
    }
    override fun id() = note.idHex + "L" + createdAt
}

class ZapSetCard(val note: Note, val zapEvents: Map<Note, Note>) : Card() {
    val createdAt = zapEvents.maxOf { it.value.createdAt() ?: 0 }
    override fun createdAt(): Long {
        return createdAt
    }
    over