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

package pl.org.seva.navigator.view.builder.dialog

import android.app.Dialog
import android.content.Context
import android.support.v7.app.AlertDialog

import pl.org.seva.navigator.R
import pl.org.seva.navigator.model.Contact

class FriendshipAddDialogBuilder(private val context: Context) {

    private lateinit var contact: Contact
    private lateinit var yesAction: () -> Unit
    private lateinit var noAction: () -> Unit

    fun setContact(contact: Contact): FriendshipAddDialogBuilder {
        this.contact = contact
        return this
    }

    fun setYesAction(yesAction: () -> Unit): FriendshipAddDialogBuilder {
        this.yesAction = yesAction
        return this
    }

    fun setNoAction(noAction: () -> Unit): FriendshipAddDialogBuilder {
        this.noAction = noAction
        return this
    }

    fun build(): Dialog = AlertDialog.Builder(context)
            .setCancelable(true)
            .setTitle(R.string.adding_friend_title)
            .setMessage(context.getString(R.string.add_friend_confirmation).replace(NAME_TAG, contact.name))
            .setPositiveButton(android.R.string.yes) { _, _ -> yesAction() }
            .setNegativeButton(android.R.string.no) { _, _ -> noAction() }
            .create()

    companion object {
        private val NAME_TAG = "[name]"
    }
}
