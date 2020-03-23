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

import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fr_seek_contact.*

import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.data.fb.fbReader
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.extension.observe
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.extension.back
import pl.org.seva.navigator.main.extension.toast
import pl.org.seva.navigator.main.extension.viewModel
import pl.org.seva.navigator.profile.loggedInUser
import java.util.*

@Suppress("DEPRECATION")
class SeekContactFragment : Fragment(R.layout.fr_seek_contact) {

    private var progress: ProgressDialog? = null

    private val searchManager get() =
        requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager

    private val navigatorModel by viewModel<NavigatorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setPromptText(R.string.seek_contact_press_to_begin)
        navigatorModel.query.observe(this) { query ->
            if (query.isNotEmpty()) {
                navigatorModel.query.value = ""
                progress = ProgressDialog.show(context, null, getString(R.string.seek_contact_searching))
                fbReader.findContact(query.toLowerCase(Locale.getDefault()))
                        .subscribe { onContactReceived(it) }
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
        fun MenuItem.prepareSearchView() = with (actionView as SearchView) {
            setOnSearchClickListener { prompt.visibility = View.GONE }
            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            setOnCloseListener {
                if (contacts_view.visibility != View.VISIBLE) {
                    prompt.visibility = View.VISIBLE
                }
                setPromptText(R.string.seek_contact_press_to_begin)
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
            prompt.visibility = View.GONE
            contacts_view.visibility = View.GONE
            requireActivity().onSearchRequested()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onContactReceived(contact: Contact) {
        fun initRecyclerView() {
            contacts_view.setHasFixedSize(true)
            val lm = LinearLayoutManager(context)
            contacts_view.layoutManager = lm
            val adapter = ContactSingleAdapter(contact) { selectedContact ->
                when {
                    selectedContact in contacts -> back()
                    selectedContact.email == loggedInUser.email ->
                        getString(R.string.seek_contact_cannot_add_yourself).toast()
                    else -> FriendshipAddDialogBuilder(requireContext())
                            .setContact(selectedContact)
                            .setYesAction { contactApprovedAndFinish(selectedContact) }
                            .setNoAction { back() }
                            .build()
                            .show()
                }
            }
            contacts_view.adapter = adapter
        }

        checkNotNull(progress).cancel()
        if (contact.isEmpty) {
            prompt.visibility = View.VISIBLE
            contacts_view.visibility = View.GONE
            setPromptText(R.string.seek_contact_no_user_found)
            return
        }
        prompt.visibility = View.GONE
        contacts_view.visibility = View.VISIBLE
        initRecyclerView()
    }

    private fun contactApprovedAndFinish(contact: Contact) {
        getString(R.string.seek_contact_waiting_for_party).toast()
        fbWriter requestFriendship contact
        back()
    }

    companion object {
        private const val IMAGE_PLACEHOLDER = "[image]"
        private const val IMAGE_PLACEHOLDER_LENGTH = IMAGE_PLACEHOLDER.length
    }
}
