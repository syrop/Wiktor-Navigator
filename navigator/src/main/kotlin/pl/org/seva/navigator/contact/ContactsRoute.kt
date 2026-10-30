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

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.data.db.contactDao
import pl.org.seva.navigator.main.data.db.delete
import pl.org.seva.navigator.main.data.db.insert
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.data.setShortcuts

@Composable
fun ContactsRoute(
    navigatorModel: NavigatorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSeekContact: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val contactsList = remember { mutableStateListOf<Contact>() }

    fun refreshContactsList() {
        contactsList.clear()
        contactsList.addAll(contacts.snapshot())
    }

    LaunchedEffect(Unit) {
        contacts.addContactsUpdatedListener {
            refreshContactsList()
        }
        refreshContactsList()
    }

    ContactsScreen(
        contactsList = contactsList,
        onContactClick = { contact ->
            navigatorModel.contact.value = contact
            onNavigateBack()
        },
        onContactSwipedToDelete = { contact ->
            fun undelete() {
                fbWriter addFriendship contact
                fbWriter acceptFriendship contact
                contacts add contact
                contactDao insert contact
                refreshContactsList()
                setShortcuts()
            }

            fbWriter deleteFriendship contact
            contacts delete contact
            contactDao delete contact
            refreshContactsList()
            setShortcuts()

            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.contacts_deleted_contact),
                    actionLabel = context.getString(R.string.contacts_undelete),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    undelete()
                }
            }
        },
        onAddContactClick = onNavigateToSeekContact,
        snackbarHostState = snackbarHostState,
    )
}
