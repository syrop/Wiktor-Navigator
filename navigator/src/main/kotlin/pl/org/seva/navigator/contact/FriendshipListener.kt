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
import pl.org.seva.navigator.main.data.ParcelableInt
import pl.org.seva.navigator.main.data.appContext

import java.util.Random

import pl.org.seva.navigator.main.data.db.contactDao
import pl.org.seva.navigator.main.data.db.delete
import pl.org.seva.navigator.main.data.db.insert
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.init.instance
import pl.org.seva.navigator.main.data.setShortcuts

val friendshipListener by instance<FriendshipListener>()

class FriendshipListener {

    private val nm by lazy {
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun onPeerRequestedFriendship(contact: Contact) {
        fun NotificationManager.friendshipRequested(contact: Contact, notificationId: ParcelableInt) =
                notify(notificationId.value, friendshipRequestedNotification(appContext) {
                    this.contact = contact
                    this.nid = notificationId
                })

        appContext.registerReceiver(FriendshipReceiver(), IntentFilter(FRIENDSHIP_REQUESTED_INTENT))
        val notificationId = ParcelableInt(Random().nextInt())
        nm.friendshipRequested(contact, notificationId)
    }

    fun onPeerAcceptedFriendship(contact: Contact) {
        contacts add contact
        contactDao insert contact
        fbWriter addFriendship contact
        setShortcuts()
    }

    fun onPeerDeletedFriendship(contact: Contact) {
        contacts delete contact
        contactDao delete contact
        setShortcuts()
    }

    private inner class FriendshipReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            fun Contact.acceptFriend() {
                fbWriter acceptFriendship this
                if (this in contacts) {
                    return
                }
                contacts add this
                contactDao insert this
            }

            val nid = intent.getParcelableExtra<ParcelableInt>(NOTIFICATION_ID).value
            nm.cancel(nid)
            context.unregisterReceiver(this)
            val action = intent.getParcelableExtra<ParcelableInt>(ACTION).value
            if (action == ACCEPTED_ACTION) {
                intent.getParcelableExtra<Contact>(CONTACT_EXTRA).acceptFriend()
                setShortcuts()
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
