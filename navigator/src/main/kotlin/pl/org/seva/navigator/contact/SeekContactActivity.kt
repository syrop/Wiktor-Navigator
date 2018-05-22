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

package pl.org.seva.navigator.contact

import android.annotation.SuppressLint
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
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_seek_contact.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.data.fb.FbReader
import pl.org.seva.navigator.data.fb.FbWriter
import pl.org.seva.navigator.main.instance
import pl.org.seva.navigator.profile.loggedInUser

@Suppress("DEPRECATION")
class SeekContactActivity : AppCompatActivity() {

    private val fbWriter: FbWriter = instance()
    private val fbReader: FbReader = instance()
    private val store: Contacts = instance()

    private var progress: ProgressDialog? = null

    private val searchManager get() = getSystemService(Context.SEARCH_SERVICE) as SearchManager

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())

        setContentView(R.layout.activity_seek_contact)

        if (Intent.ACTION_SEARCH == intent.action) {
            search(intent.getStringExtra(SearchManager.QUERY))
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        setPromptText(R.string.seek_contact_press_to_begin)
    }

    private fun setPromptText(id: Int) {
        prompt.text = getString(id).insertImage()
    }

    private fun String.insertImage(): SpannableString {
        val result = SpannableString(this)
        val d = resources.getDrawable(R.drawable.ic_search_black_24dp)
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        val span = ImageSpan(d, ImageSpan.ALIGN_BASELINE)
        val idPlaceholder = indexOf(IMAGE_PLACEHOLDER)
        if (idPlaceholder >= 0) {
            result.setSpan(
                    span,
                    idPlaceholder,
                    idPlaceholder + IMAGE_PLACEHOLDER_LENGTH,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        return result
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY).trim { it <= ' ' }
            search(query)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.seek_contact, menu)

        val searchMenuItem = menu.findItem(R.id.action_search)
        searchMenuItem.collapseActionView()
        searchMenuItem.prepareSearchView()
        return true
    }

    private fun MenuItem.prepareSearchView() = with (actionView as SearchView) {
        setOnSearchClickListener { onSearchClicked() }
        setSearchableInfo(searchManager.getSearchableInfo(componentName))
        setOnCloseListener { onSearchViewClosed() }
    }

    private fun onSearchViewClosed(): Boolean {
        if (contacts.visibility != View.VISIBLE) {
            prompt.visibility = View.VISIBLE
        }
        setPromptText(R.string.seek_contact_press_to_begin)
        return false
    }

    private fun onSearchClicked() {
        prompt.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_search -> {
            prompt.visibility = View.GONE
            contacts.visibility = View.GONE
            onSearchRequested()
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("CheckResult")
    private fun search(query: String) {
        progress = ProgressDialog.show(this, null, getString(R.string.seek_contact_searching))
        fbReader.findContact(query.toLowerCase())
                .subscribe { onContactReceived(it) }
    }

    private fun onContactReceived(contact: Contact) {
        progress!!.cancel()
        if (contact.isEmpty) {
            prompt.visibility = View.VISIBLE
            contacts.visibility = View.GONE
            setPromptText(R.string.seek_contact_no_user_found)
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
        val adapter = ContactSingleAdapter(contact) { onContactClicked(it) }
        contacts.adapter = adapter
    }

    private fun onContactClicked(contact: Contact) = when {
        contact in store -> finish()
        contact.email == loggedInUser().email ->
            Toast.makeText(this, R.string.seek_contact_cannot_add_yourself, Toast.LENGTH_SHORT).show()
        else -> FriendshipAddDialogBuilder(this)
                .setContact(contact)
                .setYesAction { contactApprovedAndFinish(contact) }
                .setNoAction { finish() }
                .build()
                .show()
    }

    private fun contactApprovedAndFinish(contact: Contact) {
        Toast.makeText(this, R.string.seek_contact_waiting_for_party, Toast.LENGTH_SHORT).show()
        fbWriter requestFriendship contact
        finish()
    }

    companion object {
        private const val IMAGE_PLACEHOLDER = "[image]"
        private const val IMAGE_PLACEHOLDER_LENGTH = IMAGE_PLACEHOLDER.length
    }
}
