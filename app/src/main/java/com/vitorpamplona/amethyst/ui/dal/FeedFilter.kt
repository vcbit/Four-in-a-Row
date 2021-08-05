package com.vitorpamplona.amethyst.ui.dal

import android.util.Log
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

abstract class FeedFilter<T>() {
    @OptIn(ExperimentalTime::class)
    fun loadTop(): List<T> 