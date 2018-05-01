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
 */

package pl.org.seva.navigator.contact

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_contacts.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.room.contactsDatabase
import pl.org.seva.navigator.navigation.NavigationActivity
import pl.org.seva.navigator.main.setDynamicShortcuts
import pl.org.seva.navigator.ui.ContactsDividerItemDecoration

import pl.org.seva.navigator.contact.room.delete
import pl.org.seva.navigator.contact.room.insert
import pl.org.seva.navigator.data.fb.fbWriter
import pl.org.seva.navigator.profile.loggedInUser

class ContactsActivity : AppCompatActivity() {

    private val store = contacts()
    private val fbWriter = fbWriter()
    private val loggedInUser = loggedInUser()
    private val contactDao = contactsDatabase().contactDao

    private var snackbar: Snackbar? = null

    private val adapter = ContactAdapter { onContactClicked(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        fun initContactsRecyclerView() {
            contacts.setHasFixedSize(true)
            contacts.layoutManager = LinearLayoutManager(this)
            contacts.adapter = adapter
            contacts.addItemDecoration(ContactsDividerItemDecoration(this))
            ItemTouchHelper(ContactTouchListener { onContactSwiped(it) }).attachToRecyclerView(contacts)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        fab.setOnClickListener { onFabClicked() }

        store.addContactsUpdatedListener { onContactsUpdatedInStore() }

        supportActionBar!!.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }
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
        startActivity(Intent(this, SeekContactActivity::class.java))
    }

    private fun onContactClicked(contact: Contact) {
        val intent = Intent(this, NavigationActivity::class.java)

        if (contact.email != loggedInUser.email) {
            intent.putExtra(NavigationActivity.CONTACT_EXTRA, contact)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
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
        setDynamicShortcuts(this)
    }

    private fun undeleteFriend(contact: Contact) {
        fbWriter addFriendship contact
        fbWriter acceptFriendship contact
        store add contact
        contactDao insert contact
        adapter.notifyDataSetChanged()
        setDynamicShortcuts(this)
    }

    private fun setPromptLabelText() {
        val str = getString(R.string.contacts_please_press_plus)
        val idPlus = str.indexOf('+')
        val boldSpan = StyleSpan(Typeface.BOLD)
        val ssBuilder = SpannableStringBuilder(str)
        ssBuilder.setSpan(boldSpan, idPlus, idPlus + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        prompt.text = ssBuilder
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
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
