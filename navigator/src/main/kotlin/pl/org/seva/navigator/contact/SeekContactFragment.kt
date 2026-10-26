/*
 * Copyright (C) 2017 Wiktor Nizio
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

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.data.fb.fbReader
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.extension.back
import pl.org.seva.navigator.main.extension.observe
import pl.org.seva.navigator.main.extension.toast
import pl.org.seva.navigator.main.extension.viewModel
import pl.org.seva.navigator.main.ui.NavigatorTheme
import pl.org.seva.navigator.profile.loggedInUser
import java.util.Locale

class SeekContactFragment : Fragment() {

    private val searchManager get() =
        requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager

    private val navigatorModel by viewModel<NavigatorViewModel>()

    private val uiState = mutableStateOf<SeekContactUiState>(
        SeekContactUiState.Prompt(SeekPromptType.PRESS_TO_BEGIN),
    )

    private val confirmationContact = mutableStateOf<Contact?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NavigatorTheme {
                SeekContactScreen(
                    uiState = uiState.value,
                    onContactClick = { contact -> onContactSelected(contact) },
                    confirmationContact = confirmationContact.value,
                    onConfirmAddContact = { contact ->
                        confirmationContact.value = null
                        contactApprovedAndFinish(contact)
                    },
                    onDismissConfirmation = {
                        confirmationContact.value = null
                        back()
                    },
                )
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        uiState.value = SeekContactUiState.Prompt(SeekPromptType.PRESS_TO_BEGIN)
        navigatorModel.query.observe(this) { query ->
            if (query.isNotEmpty()) {
                navigatorModel.query.value = ""
                uiState.value = SeekContactUiState.Searching
                fbReader.findContact(query.lowercase(Locale.getDefault()))
                        .subscribe { onContactReceived(it) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        fun MenuItem.prepareSearchView() = with (actionView as SearchView) {
            setOnSearchClickListener { uiState.value = SeekContactUiState.Hidden }
            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            setOnCloseListener {
                if (uiState.value !is SeekContactUiState.ContactResult) {
                    uiState.value = SeekContactUiState.Prompt(SeekPromptType.PRESS_TO_BEGIN)
                }
                false
            }
        }

        menuInflater.inflate(R.menu.seek_contact, menu)

        val searchMenuItem = menu.findItem(R.id.action_search)
        searchMenuItem.collapseActionView()
        searchMenuItem.prepareSearchView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_search -> {
            uiState.value = SeekContactUiState.Hidden
            requireActivity().onSearchRequested()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onContactReceived(contact: Contact) {
        if (contact.isEmpty) {
            uiState.value = SeekContactUiState.Prompt(SeekPromptType.NO_USER_FOUND)
            return
        }
        uiState.value = SeekContactUiState.ContactResult(contact)
    }

    private fun onContactSelected(selectedContact: Contact) {
        when {
            selectedContact in contacts -> back()
            selectedContact.email == loggedInUser.email ->
                getString(R.string.seek_contact_cannot_add_yourself).toast()
            else -> confirmationContact.value = selectedContact
        }
    }

    private fun contactApprovedAndFinish(contact: Contact) {
        getString(R.string.seek_contact_waiting_for_party).toast()
        fbWriter requestFriendship contact
        back()
    }
}
