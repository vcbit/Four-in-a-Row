package com.vitorpamplona.amethyst.ui.screen

import androidx.lifecycle.ViewModel
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.ServiceManager
import com.vitorpamplona.amethyst.model.Account
import fr.acinq.secp256k1.Hex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nostr.postr.Persona
import nostr.postr.bechToBytes
import java.util.regex.Pattern

class AccountStateViewModel() : ViewModel() {
    private val _accountContent = MutableStateFlow<AccountState>(AccountState.LoggedOff)
    val accountContent = _accountContent.asStateFlow()

    init {
        // pulls account from storage.

        // Keeps it in the the UI thread to void blinking the login page.
        // viewModelScope.launch(Dispatchers.IO