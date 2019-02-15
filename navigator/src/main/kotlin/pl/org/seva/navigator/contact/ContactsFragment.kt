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

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_contacts.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.setShortcuts
import pl.org.seva.navigator.ui.ContactsDividerItemDecoration

import pl.org.seva.navigator.main.fb.fbWriter
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.db.contactDao
import pl.org.seva.navigator.main.db.insert
import pl.org.seva.navigator.main.db.delete
import pl.org.seva.navigator.main.extension.navigate
import pl.org.seva.navigator.main.extension.back
import pl.org.seva.navigator.main.extension.viewModel

class ContactsFragment : Fragment() {

    private val adapter = ContactAdapter { contact ->
        navigatorModel.contact.value = contact
        back()
    }

    private val navigatorModel by viewModel<NavigatorViewModel>()

    private fun Contact.delete(onChange: () -> Unit) {
        fun undelete() {
            fbWriter addFriendship this
            fbWriter acceptFriendship this
            contacts add this
            contactDao insert this
            onChange()
        }

        fbWriter deleteFriendship this
        contacts delete this
        contactDao delete this
        Snackbar.make(
                contacts_view,
                R.string.contacts_deleted_contact,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.contacts_undelete) { undelete() }
                .show()
        onChange()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun initContactsRecyclerView() {
            contacts_view.setHasFixedSize(true)
            contacts_view.layoutManager = LinearLayoutManager(context)
            contacts_view.adapter = adapter
            contacts_view.addItemDecoration(ContactsDividerItemDecoration(context!!))
            ItemTouchHelper(ContactTouchListener { position ->
                contacts[position].delete {
                    refreshScreen()
                    adapter.notifyDataSetChanged()
                    setShortcuts()
                }
            }).attachToRecyclerView(contacts_view)
        }

        fun setPromptLabel() {
            val str = getString(R.string.contacts_please_press_plus)
            val idPlus = str.indexOf('+')
            val boldSpan = StyleSpan(Typeface.BOLD)
            val ssBuilder = SpannableStringBuilder(str)
            ssBuilder.setSpan(boldSpan, idPlus, idPlus + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            prompt.text = ssBuilder
        }

        seek_contact_fab.setOnClickListener {
            navigate(R.id.action_contactsFragment_to_seekContactFragment)
        }

        contacts.addContactsUpdatedListener {
            adapter.notifyDataSetChanged()
            refreshScreen()
        }

        setPromptLabel()
        initContactsRecyclerView()
        refreshScreen()
    }

    private fun refreshScreen() {
        if (contacts.size > 0) {
            contacts_view.visibility = View.VISIBLE
            prompt.visibility = View.GONE
        }
        else {
            contacts_view.visibility = View.GONE
            prompt.visibility = View.VISIBLE
        }
    }
}
