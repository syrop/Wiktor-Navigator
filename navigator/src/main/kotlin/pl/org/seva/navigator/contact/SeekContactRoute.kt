/*
 * Copyright (C) 2026 Wiktor Nizio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.navigator.contact

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.reactivex.disposables.CompositeDisposable
import java.util.Locale
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.data.fb.fbReader
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.extension.toast
import pl.org.seva.navigator.profile.loggedInUser

@Composable
fun SeekContactRoute(
    navigatorModel: NavigatorViewModel,
    isSearchActive: Boolean,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var uiState by remember {
        mutableStateOf<SeekContactUiState>(
            value = SeekContactUiState.Prompt(SeekPromptType.PRESS_TO_BEGIN),
        )
    }
    var confirmationContact by remember { mutableStateOf<Contact?>(value = null) }

    val effectiveUiState = if (isSearchActive && uiState is SeekContactUiState.Prompt) {
        SeekContactUiState.Hidden
    } else {
        uiState
    }

    DisposableEffect(lifecycleOwner) {
        val disposables = CompositeDisposable()
        val queryObserver = Observer<String> { query ->
            if (query.isNotEmpty()) {
                navigatorModel.query.value = ""
                uiState = SeekContactUiState.Searching
                val disposable = fbReader.findContact(query.lowercase(Locale.getDefault()))
                    .subscribe { contact ->
                        uiState = if (contact.isEmpty) {
                            SeekContactUiState.Prompt(SeekPromptType.NO_USER_FOUND)
                        } else {
                            SeekContactUiState.ContactResult(contact)
                        }
                    }
                disposables.add(disposable)
            }
        }
        navigatorModel.query.observe(lifecycleOwner, queryObserver)
        onDispose {
            navigatorModel.query.removeObserver(queryObserver)
            disposables.clear()
        }
    }

    SeekContactScreen(
        uiState = effectiveUiState,
        onContactClick = { selectedContact ->
            when {
                selectedContact in contacts -> onNavigateBack()
                selectedContact.email == loggedInUser.email ->
                    context.getString(R.string.seek_contact_cannot_add_yourself).toast()
                else -> confirmationContact = selectedContact
            }
        },
        confirmationContact = confirmationContact,
        onConfirmAddContact = { contact ->
            confirmationContact = null
            context.getString(R.string.seek_contact_waiting_for_party).toast()
            fbWriter requestFriendship contact
            onNavigateBack()
        },
        onDismissConfirmation = {
            confirmationContact = null
            onNavigateBack()
        },
    )
}
