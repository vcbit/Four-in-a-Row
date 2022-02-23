package com.vitorpamplona.amethyst.ui.note

import android.content.Context
import android.text.format.DateUtils
import com.vitorpamplona.amethyst.R

fun timeAgo(mills: Long?, context: Context): String {
    if (mills == null) return " "
    if (mills == 0L) return " • ${context.getString(R.string.never)}"

    var humanReadable = DateUtils.getRelativeTimeSpanString(
        mills * 1000,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    ).toString()
    if (humanReadable.startsWith("In") || humanReadable.startsW