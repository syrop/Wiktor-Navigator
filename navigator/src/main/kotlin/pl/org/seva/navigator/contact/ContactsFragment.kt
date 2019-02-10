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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_contacts.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.db.contactsDatabase
import pl.org.seva.navigator.main.setDynamicShortcuts
import pl.org.seva.navigator.ui.ContactsDividerItemDecoration

import pl.org.seva.navigator.main.db.delete
import pl.org.seva.navigator.main.db.insert
import pl.org.seva.navigator.main.fb.fbWriter
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.extension.navigate
import pl.org.seva.navigator.main.extension.popBackStack

class ContactsFragment : Fragment() {

    private val contactDao = contactsDatabase.contactDao

    private var snackbar: Snackbar? = null

    private val adapter = ContactAdapter { onContactClicked(it) }

    private lateinit var navigatorModel: NavigatorViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun initContactsRecyclerView() {
            contacts_view.setHasFixedSize(true)
            contacts_view.layoutManager = LinearLayoutManager(context)
            contacts_view.adapter = adapter
            contacts_view.addItemDecoration(ContactsDividerItemDecoration(context!!))
            ItemTouchHelper(ContactTouchListener { onContactSwiped(it) })
                    .attachToRecyclerView(contacts_view)
        }

        navigatorModel = ViewModelProviders.of(activity!!).get(NavigatorViewModel::class.java)
        fab.setOnClickListener { onFabClicked() }

        contacts.addContactsUpdatedListener { onContactsUpdatedInStore() }

        setPromptLabelText()
        initContactsRecyclerView()
        promptOrRecyclerView()
    }

    private fun promptOrRecyclerView()  {
        if (contacts.size > 0) {
            contacts_view.visibility = View.VISIBLE
            prompt.visibility = View.GONE
        }
        else {
            contacts_view.visibility = View.GONE
            prompt.visibility = View.VISIBLE
        }
    }

    private fun onFabClicked() {
        navigate(R.id.action_contactsFragment_to_seekContactFragment)
    }

    private fun onContactClicked(contact: Contact) {
        navigatorModel.contact.value = contact
        popBackStack()
    }

    private fun onContactSwiped(position: Int) {
        val contact = contacts[position]
        deleteFriend(contact)
        promptOrRecyclerView()
    }

    private fun onUndeleteClicked(contact: Contact) {
        undeleteFriend(contact)
        promptOrRecyclerView()
    }

    private fun deleteFriend(contact: Contact) {
        fbWriter deleteFriendship contact
        contacts delete contact
        contactDao delete contact
        adapter.notifyDataSetChanged()
        showUndeleteSnackbar(contact)
        setDynamicShortcuts(context!!)
    }

    private fun undeleteFriend(contact: Contact) {
        fbWriter addFriendship contact
        fbWriter acceptFriendship contact
        contacts add contact
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
                contacts_view,
                R.string.contacts_deleted_contact,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.contacts_undelete) { onUndeleteClicked(contact) }
        snackbar!!.show()
    }
}
