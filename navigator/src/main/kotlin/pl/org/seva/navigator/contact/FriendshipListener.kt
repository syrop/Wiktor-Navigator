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

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import pl.org.seva.navigator.main.*

import java.util.Random

import pl.org.seva.navigator.main.db.contactDao
import pl.org.seva.navigator.main.db.delete
import pl.org.seva.navigator.main.db.insert
import pl.org.seva.navigator.main.fb.fbWriter

val friendshipListener by instance<FriendshipListener>()

class FriendshipListener {

    private val nm get() = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun onPeerRequestedFriendship(contact: Contact) {
        appContext.registerReceiver(FriendshipReceiver(), IntentFilter(FRIENDSHIP_REQUESTED_INTENT))
        val notificationId = ParcelableInt(Random().nextInt())
        nm.friendshipRequested(contact, notificationId)
    }

    fun onPeerAcceptedFriendship(contact: Contact) {
        addContact(contact)
        contactDao insert contact
        fbWriter addFriendship contact
        setDynamicShortcuts(appContext)
    }

    fun onPeerDeletedFriendship(contact: Contact) {
        deleteContact(contact)
        contactDao delete contact
        setDynamicShortcuts(appContext)
    }

    private fun acceptFriend(contact: Contact) {
        fbWriter acceptFriendship contact
        if (contact in contacts) {
            return
        }
        addContact(contact)
        contactDao insert contact
    }

    private fun NotificationManager.friendshipRequested(contact: Contact, notificationId: ParcelableInt) =
            notify(notificationId.value, friendshipRequestedNotification(appContext) {
                this.contact = contact
                this.nid = notificationId
            })

    private inner class FriendshipReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val nid = intent.getParcelableExtra<ParcelableInt>(NOTIFICATION_ID).value
            nm.cancel(nid)
            context.unregisterReceiver(this)
            val contact = intent.getParcelableExtra<Contact>(CONTACT_EXTRA)
            val action = intent.getParcelableExtra<ParcelableInt>(ACTION).value
            if (action == ACCEPTED_ACTION) {
                acceptFriend(contact)
                setDynamicShortcuts(context)
            }
        }
    }

    companion object {

        const val FRIENDSHIP_REQUESTED_INTENT = "friendship_requested_intent"
        const val NOTIFICATION_ID = "notification_id"
        const val ACTION = "friendship_action"
        const val CONTACT_EXTRA = "contact_extra"
        const val ACCEPTED_ACTION = 0
        const val REJECTED_ACTION = 1
    }
}
