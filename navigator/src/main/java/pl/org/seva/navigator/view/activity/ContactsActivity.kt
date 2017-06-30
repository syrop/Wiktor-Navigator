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

package pl.org.seva.navigator.view.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import javax.inject.Inject

import pl.org.seva.navigator.NavigatorApplication
import pl.org.seva.navigator.R
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.Login
import pl.org.seva.navigator.model.database.firebase.FirebaseWriter
import pl.org.seva.navigator.model.database.sqlite.SqlWriter
import pl.org.seva.navigator.presenter.ContactTouchHelperCallback
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.view.adapter.ContactAdapter

class ContactsActivity : AppCompatActivity() {

    @Inject
    lateinit var myLocationSource: MyLocationSource
    @Inject
    lateinit var contactsStore: ContactsStore
    @Inject
    lateinit var firebaseWriter: FirebaseWriter
    @Inject
    lateinit var login: Login
    @Inject
    lateinit var sqlWriter: SqlWriter

    private val contacts by lazy { findViewById<RecyclerView>(R.id.contacts) }
    private val adapter = ContactAdapter()
    private val component by lazy { (application as NavigatorApplication).component }
    private val fab by lazy { findViewById<View>(R.id.fab) }
    private val prompt by lazy { findViewById<TextView>(R.id.prompt)}
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_contacts)
        fab.setOnClickListener { onFabClicked() }

        contactsStore.addContactsUpdatedListener { onContactsUpdated() }

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }
        setPromptLabelText()
        initContactsRecyclerView()
        promptOrRecyclerView()
    }

    private fun promptOrRecyclerView() {
        if (contactsStore.size() > 0) {
            contacts.visibility = View.VISIBLE
            prompt.visibility = View.GONE
        } else {
            contacts.visibility = View.GONE
            prompt.visibility = View.VISIBLE
        }
    }

    private fun onFabClicked() {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    private fun initContactsRecyclerView() {
        contacts.setHasFixedSize(true)
        contacts.layoutManager = LinearLayoutManager(this)
        component.inject(adapter)
        adapter.addClickListener { onContactClicked(it) }
        contacts.adapter = adapter
        ItemTouchHelper(ContactTouchHelperCallback { onContactSwiped(it) } )
                .attachToRecyclerView(contacts)
    }

    private fun onContactClicked(contact: Contact) {
        val intent = Intent(this, NavigationActivity::class.java)

        if (contact.email!! != login.email) {
            intent.putExtra(NavigationActivity.CONTACT, contact)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun onContactSwiped(position: Int) {
        val contact = contactsStore[position]
        deleteFriend(contact)
        promptOrRecyclerView()
    }

    private fun onUndeleteClicked(contact: Contact) {
        undeleteFriend(contact)
        promptOrRecyclerView()
    }

    private fun deleteFriend(contact: Contact) {
        firebaseWriter.deleteFriendship(contact)
        contactsStore.delete(contact)
        sqlWriter.deleteFriend(contact)
        adapter.notifyDataSetChanged()
        showUndeleteSnackbar(contact)
    }

    private fun undeleteFriend(contact: Contact) {
        firebaseWriter.addFriendship(contact)
        firebaseWriter.acceptFriendship(contact)
        contactsStore.add(contact)
        sqlWriter.addFriend(contact)
        adapter.notifyDataSetChanged()
    }

    private fun setPromptLabelText() {
        val str = getString(R.string.contacts_please_press_plus)
        val idPlus = str.indexOf('+')
        val boldSpan = StyleSpan(Typeface.BOLD)
        val ssBuilder = SpannableStringBuilder(str)
        ssBuilder.setSpan(boldSpan, idPlus, idPlus + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        prompt.text = ssBuilder
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun onContactsUpdated() {
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
