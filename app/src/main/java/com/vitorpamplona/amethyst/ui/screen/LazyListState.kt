
package com.vitorpamplona.amethyst.ui.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.vitorpamplona.amethyst.ui.navigation.Route

private val savedScrollStates = mutableMapOf<String, ScrollState>()
private data class ScrollState(val index: Int, val scrollOffset: Int)

object ScrollStateKeys {
    const val GLOBAL_SCREEN = "Global"
    val HOME_FOLLOWS = Route.Home.base + "Follows"
    val HOME_REPLIES = Route.Home.base + "FollowsReplies"
}

@Composable
fun rememberForeverLazyListState(
    key: String,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyListState {
    val scrollState = rememberSaveable(saver = LazyListState.Saver) {
        val savedValue = savedScrollStates[key]
        val savedIndex = savedValue?.index ?: initialFirstVisibleItemIndex
        val savedOffset = savedValue?.scrollOffset ?: initialFirstVisibleItemScrollOffset
        LazyListState(
            savedIndex,
            savedOffset
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            val lastIndex = scrollState.firstVisibleItemIndex
            val lastOffset = scrollState.firstVisibleItemScrollOffset
            savedScrollStates[key] = ScrollState(lastIndex, lastOffset)
        }
    }
    return scrollState
}