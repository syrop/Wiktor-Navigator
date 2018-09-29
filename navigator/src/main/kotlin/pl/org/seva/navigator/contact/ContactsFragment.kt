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
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_contacts.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.room.contactsDatabase
import pl.org.seva.navigator.main.setDynamicShortcuts
import pl.org.seva.navigator.ui.ContactsDividerItemDecoration

import pl.org.seva.navigator.contact.room.delete
import pl.org.seva.navigator.contact.room.insert
import pl.org.seva.navigator.data.fb.fbWriter
import pl.org.seva.navigator.navigation.NavigationViewModel

class ContactsFragment : Fragment() {

    private val store = contactsStore
    private val contactDao = contactsDatabase.contactDao

    private var snackbar: Snackbar? = null

    private val adapter = ContactAdapter { onContactClicked(it) }

    private lateinit var navigationModel: NavigationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun initContactsRecyclerView() {
            contacts.setHasFixedSize(true)
            contacts.layoutManager = LinearLayoutManager(context)
            contacts.adapter = adapter
            contacts.addItemDecoration(ContactsDividerItemDecoration(context!!))
            ItemTouchHelper(ContactTouchListener { onContactSwiped(it) }).attachToRecyclerView(contacts)
        }

        navigationModel = ViewModelProviders.of(this).get(NavigationViewModel::class.java)
        fab.setOnClickListener { onFabClicked() }

        store.addContactsUpdatedListener { onContactsUpdatedInStore() }

        setPromptLabelText()
        initContactsRecyclerView()
        promptOrRecyclerView()
    }

    private fun promptOrRecyclerView() = if (store.size() > 0) {
        contacts.visibility = View.VISIBLE
        prompt.visibility = View.GONE
    } else {
        contacts.visibility = View.GONE
        prompt.visibility = View.VISIBLE
    }

    private fun onFabClicked() {
        findNavController().navigate(R.id.action_navigationFragment_to_contactsFragment)
    }

    private fun onContactClicked(contact: Contact) {
        navigationModel.contact.value = contact
        findNavController().popBackStack()
    }

    private fun onContactSwiped(position: Int) {
        val contact = store[position]
        deleteFriend(contact)
        promptOrRecyclerView()
    }

    private fun onUndeleteClicked(contact: Contact) {
        undeleteFriend(contact)
        promptOrRecyclerView()
    }

    private fun deleteFriend(contact: Contact) {
        fbWriter deleteFriendship contact
        store delete contact
        contactDao delete contact
        adapter.notifyDataSetChanged()
        showUndeleteSnackbar(contact)
        setDynamicShortcuts(context!!)
    }

    private fun undeleteFriend(contact: Contact) {
        fbWriter addFriendship contact
        fbWriter acceptFriendship contact
        store add contact
        contactDao insert contact
        adapter.notifyDataSetChanged()
        setDynamicShortcuts(context!!)
    }

    private fun setPromptLabelText() {
        val str = getString(R.string.contacts_please_press_plus)
        val idPlus = str.indexOf('+')
        val boldSpan = StyleSpan(Typeface.BOLD)
        val ssBuilder = SpannableStringBuilder(str)
        ssBuilder.setSpan(boldSpan, idPlus, idPlus + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        prompt.text = ssBuilder
    }

    private fun onContactsUpdatedInStore() {
        adapter.notifyDataSetChanged()
        promptOrRecyclerView()
    }

    private fun showUndeleteSnackbar(contact: Contact) {
        snackbar = Snackbar.make(
                contacts,
                R.string.contacts_deleted_contact,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.contacts_undelete) { onUndeleteClicked(contact) }
        snackbar!!.show()
    }
}
