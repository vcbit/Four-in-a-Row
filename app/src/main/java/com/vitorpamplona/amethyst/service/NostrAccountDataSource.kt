package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.service.model.BadgeAwardEvent
import com.vitorpamplona.amethyst.service.model.BadgeProfilesEvent
import com.vitorpamplona.amethyst.service.model.ChannelMessageEvent
import com.vitorpamplona.amethyst.service.model.ContactListEvent
import com.vitorpamplona.amethyst.service.model.LnZapEvent
import com.vitorpamplona.amethyst.service.model.MetadataEvent
import com.vitorpamplona.amethyst.service.model.ReactionEvent
import com.vitorpamplona.amethyst.service.model.ReportEvent
import com.vitorpamplona.amethyst.service.model.RepostEvent
import com.vitorpamplona.amethyst.service.model.TextNoteEvent
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter

object NostrAccountDataSource : NostrDataSou