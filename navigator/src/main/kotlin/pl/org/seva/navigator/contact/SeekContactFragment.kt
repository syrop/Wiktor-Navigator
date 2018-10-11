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

@file:Suppress("DEPRECATION")

package pl.org.seva.navigator.contact

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_seek_contact.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.data.fb.fbReader
import pl.org.seva.navigator.data.fb.fbWriter
import pl.org.seva.navigator.main.observe
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.profile.loggedInUser

@Suppress("DEPRECATION")
class SeekContactFragment : Fragment() {

    private var progress: ProgressDialog? = null

    private val searchManager get() = context!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager

    private lateinit var navigatorModel: NavigatorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_seek_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navigatorModel = ViewModelProviders.of(activity!!).get(NavigatorViewModel::class.java)

        setPromptText(R.string.seek_contact_press_to_begin)
        navigatorModel.query.observe(this) {
            if (!it.isEmpty()) {
                navigatorModel.query.value = ""
                search(it)
            }
        }
    }

    private fun setPromptText(id: Int) {
        fun String.insertSearchImage(): SpannableString {
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

        prompt.text = getString(id).insertSearchImage()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.seek_contact, menu)

        val searchMenuItem = menu.findItem(R.id.action_search)
        searchMenuItem.collapseActionView()
        searchMenuItem.prepareSearchView()
    }

    private fun MenuItem.prepareSearchView() = with (actionView as SearchView) {
        setOnSearchClickListener { onSearchClicked() }
        setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))
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
            activity!!.onSearchRequested()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("CheckResult")
    private fun search(query: String) {
        progress = ProgressDialog.show(context, null, getString(R.string.seek_contact_searching))
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
        val lm = LinearLayoutManager(context)
        contacts.layoutManager = lm
        val adapter = ContactSingleAdapter(contact) { onContactClicked(it) }
        contacts.adapter = adapter
    }

    private fun onContactClicked(contact: Contact) = when {
        contact in contactsStore -> {
            findNavController().popBackStack()
            Unit
        }
        contact.email == loggedInUser.email ->
            Toast.makeText(activity, R.string.seek_contact_cannot_add_yourself, Toast.LENGTH_SHORT).show()
        else -> FriendshipAddDialogBuilder(context!!)
                .setContact(contact)
                .setYesAction { contactApprovedAndFinish(contact) }
                .setNoAction { findNavController().popBackStack() }
                .build()
                .show()
    }

    private fun contactApprovedAndFinish(contact: Contact) {
        Toast.makeText(activity, R.string.seek_contact_waiting_for_party, Toast.LENGTH_SHORT).show()
        fbWriter requestFriendship contact
        findNavController().popBackStack()
    }

    companion object {
        private const val IMAGE_PLACEHOLDER = "[image]"
        private const val IMAGE_PLACEHOLDER_LENGTH = IMAGE_PLACEHOLDER.length
    }
}
