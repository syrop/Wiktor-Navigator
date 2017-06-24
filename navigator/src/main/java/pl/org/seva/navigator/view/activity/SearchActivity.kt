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

@file:Suppress("DEPRECATION")

package pl.org.seva.navigator.view.activity

import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import javax.inject.Inject

import pl.org.seva.navigator.R
import pl.org.seva.navigator.NavigatorApplication
import pl.org.seva.navigator.model.ContactsCache
import pl.org.seva.navigator.model.database.firebase.FirebaseReader
import pl.org.seva.navigator.model.database.firebase.FirebaseWriter
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.view.adapter.SingleContactAdapter
import pl.org.seva.navigator.view.builder.dialog.FriendshipAddDialogBuilder

@Suppress("DEPRECATION")
class SearchActivity : AppCompatActivity() {

    @Inject
    lateinit var firebaseWriter: FirebaseWriter
    @Inject
    lateinit var firebaseReader: FirebaseReader
    @Inject
    lateinit var contactsCache: ContactsCache

    private val promptLabel by lazy { findViewById<TextView>(R.id.promptLabel) }
    private val contacts by lazy { findViewById<RecyclerView>(R.id.contacts) }

    private var progress: ProgressDialog? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as NavigatorApplication).component.inject(this)

        setContentView(R.layout.activity_search)

        if (Intent.ACTION_SEARCH == intent.action) {
            search(intent.getStringExtra(SearchManager.QUERY))
        }

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        setPromptLabelText(R.string.search_press_to_begin)
    }

    // http://stackoverflow.com/questions/3176033/spannablestring-with-image-example
    private fun setPromptLabelText(id: Int) {
        val str = getString(id)
        val ss = SpannableString(str)
        val d = resources.getDrawable(R.drawable.ic_search_black_24dp)
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        val span = ImageSpan(d, ImageSpan.ALIGN_BASELINE)
        val idPlaceholder = str.indexOf(IMAGE_PLACEHOLDER)
        if (idPlaceholder >= 0) {
            ss.setSpan(
                    span,
                    idPlaceholder,
                    idPlaceholder + IMAGE_PLACEHOLDER_LENGTH,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        promptLabel!!.text = ss
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY).trim { it <= ' ' }
            search(query)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_overflow_menu, menu)

        val searchMenuItem = menu.findItem(R.id.action_search)
        searchMenuItem.collapseActionView()
        val searchView = searchMenuItem.actionView as SearchView
        searchView.setOnSearchClickListener { onSearchClicked() }

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        searchView.setOnCloseListener { onSearchViewClosed() }
        return true
    }

    private fun onSearchViewClosed(): Boolean {
        if (contacts!!.visibility != View.VISIBLE) {
            promptLabel!!.visibility = View.VISIBLE
        }
        setPromptLabelText(R.string.search_press_to_begin)
        return false
    }

    private fun onSearchClicked() {
        promptLabel!!.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                promptLabel!!.visibility = View.GONE
                contacts!!.visibility = View.GONE
                onSearchRequested()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun search(query: String) {
        val localQuery = query.toLowerCase()
        progress = ProgressDialog.show(this, null, getString(R.string.search_searching))
        firebaseReader
                .readContactOnceForEmail(localQuery)
                .subscribe { onContactReceived(it) }
    }

    private fun onContactReceived(contact: Contact) {
        progress!!.cancel()
        if (contact.isEmpty) {
            promptLabel!!.visibility = View.VISIBLE
            contacts!!.visibility = View.GONE
            setPromptLabelText(R.string.search_no_user_found)
            return
        }
        promptLabel!!.visibility = View.GONE
        contacts!!.visibility = View.VISIBLE
        initRecyclerView(contact)
    }

    private fun initRecyclerView(contact: Contact) {
        contacts!!.setHasFixedSize(true)
        val lm = LinearLayoutManager(this)
        contacts!!.layoutManager = lm
        val adapter = SingleContactAdapter(contact)
        contacts!!.adapter = adapter
        adapter.addClickListener { onContactClicked(it) }
    }

    private fun onContactClicked(contact: Contact) {
        if (contactsCache.contains(contact)) {
            finish()
            return
        }
        FriendshipAddDialogBuilder(this)
                .setContact(contact)
                .setYesAction { contactApprovedAndFinish(contact) }
                .setNoAction { finish() }
                .build()
                .show()
    }

    private fun contactApprovedAndFinish(contact: Contact) {
        Toast.makeText(this, R.string.search_waiting_for_party, Toast.LENGTH_SHORT).show()
        firebaseWriter.requestFriendship(contact)
        finish()
    }

    companion object {

        private val IMAGE_PLACEHOLDER = "[image]"
        private val IMAGE_PLACEHOLDER_LENGTH = IMAGE_PLACEHOLDER.length
    }
}
