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
import android.support.v7.widget.SearchView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_search.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.firebase.FbReader
import pl.org.seva.navigator.model.firebase.FbWriter
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.Login
import pl.org.seva.navigator.view.adapter.SingleContactAdapter
import pl.org.seva.navigator.view.builder.dialog.FriendshipAddDialogBuilder

@Suppress("DEPRECATION")
class SearchActivity: AppCompatActivity(), KodeinGlobalAware {

    private val fbWriter: FbWriter = instance()
    private val fbReader: FbReader = instance()
    private val store: ContactsStore = instance()
    private val login: Login = instance()

    private var progress: ProgressDialog? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

        if (Intent.ACTION_SEARCH == intent.action) {
            search(intent.getStringExtra(SearchManager.QUERY))
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        setPromptLabelText(R.string.search_press_to_begin)
    }

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
        prompt.text = ss
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY).trim { it <= ' ' }
            search(query)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)

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
        if (contacts.visibility != View.VISIBLE) {
            prompt.visibility = View.VISIBLE
        }
        setPromptLabelText(R.string.search_press_to_begin)
        return false
    }

    private fun onSearchClicked() {
        prompt.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                prompt.visibility = View.GONE
                contacts.visibility = View.GONE
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
        progress = ProgressDialog.show(this, null, getString(R.string.search_searching))
        fbReader
                .findContact(query.toLowerCase())
                .subscribe { onContactReceived(it) }
    }

    private fun onContactReceived(contact: Contact) {
        progress!!.cancel()
        if (contact.isEmpty) {
            prompt.visibility = View.VISIBLE
            contacts.visibility = View.GONE
            setPromptLabelText(R.string.search_no_user_found)
            return
        }
        prompt.visibility = View.GONE
        contacts.visibility = View.VISIBLE
        initRecyclerView(contact)
    }

    private fun initRecyclerView(contact: Contact) {
        contacts.setHasFixedSize(true)
        val lm = LinearLayoutManager(this)
        contacts.layoutManager = lm
        val adapter = SingleContactAdapter(contact)
        contacts.adapter = adapter
        adapter.addClickListener { onContactClicked(it) }
    }

    private fun onContactClicked(contact: Contact) {
        if (store.contains(contact)) {
            finish()
        }
        else if (contact.email == login.email) {
            Toast.makeText(this, R.string.search_cannot_add_yourself, Toast.LENGTH_SHORT).show()
        }
        else {
            FriendshipAddDialogBuilder(this)
                    .setContact(contact)
                    .setYesAction { contactApprovedAndFinish(contact) }
                    .setNoAction { finish() }
                    .build()
                    .show()
        }
    }

    private fun contactApprovedAndFinish(contact: Contact) {
        Toast.makeText(this, R.string.search_waiting_for_party, Toast.LENGTH_SHORT).show()
        fbWriter.requestFriendship(contact)
        finish()
    }

    companion object {
        private val IMAGE_PLACEHOLDER = "[image]"
        private val IMAGE_PLACEHOLDER_LENGTH = IMAGE_PLACEHOLDER.length
    }
}
