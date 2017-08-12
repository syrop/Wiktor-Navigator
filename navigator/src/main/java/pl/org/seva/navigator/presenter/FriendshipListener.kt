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

package pl.org.seva.navigator.presenter

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance

import java.lang.ref.WeakReference
import java.util.Random

import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.firebase.FbWriter
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.ParcelableInt
import pl.org.seva.navigator.model.room.ContactsDatabase
import pl.org.seva.navigator.view.builder.notification.friendshipRequestedNotification

class FriendshipListener: KodeinGlobalAware {

    private val store: ContactsStore = instance()
    private val contactDao = instance<ContactsDatabase>().contactDao
    private val fbWriter: FbWriter = instance()

    private lateinit var weakContext: WeakReference<Context>

    fun init(context: Context) {
        weakContext = WeakReference(context)
    }

    fun onPeerRequestedFriendship(contact: Contact) {
        val context = weakContext.get() ?: return
        val acceptedReceiver = FriendshipRequestedBroadcastReceiver()
        context.registerReceiver(acceptedReceiver, IntentFilter(FRIENDSHIP_REQUESTED_INTENT))
        val notificationId = ParcelableInt(Random().nextInt())
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId.value, friendshipRequestedNotification(context) {
            contact(contact)
            notificationId(notificationId)
        })
    }

    fun onPeerAcceptedFriendship(contact: Contact) {
        store.add(contact)
        contactDao.insert(contact)
        fbWriter.addFriendship(contact)
    }

    fun onPeerDeletedFriendship(contact: Contact) {
        store.delete(contact)
        contactDao.delete(contact)
    }

    private fun acceptFriend(contact: Contact) {
        fbWriter.acceptFriendship(contact)
        if (store.contains(contact)) {
            return
        }
        store.add(contact)
        contactDao.insert(contact)
    }

    private inner class FriendshipRequestedBroadcastReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val notificationId = intent.getParcelableExtra<ParcelableInt>(NOTIFICATION_ID).value
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.cancel(notificationId)
            context.unregisterReceiver(this)
            val contact = intent.getParcelableExtra<Contact>(CONTACT_EXTRA)
            val action = intent.getParcelableExtra<ParcelableInt>(ACTION).value
            if (action == ACCEPTED_ACTION) {
                acceptFriend(contact)
            }
        }
    }

    companion object {

        val FRIENDSHIP_REQUESTED_INTENT = "friendship_requested_intent"
        val NOTIFICATION_ID = "notification_id"
        val ACTION = "action"
        val CONTACT_EXTRA = "contact_extra"
        val ACCEPTED_ACTION = 0
        val REJECTED_ACTION = 1
    }
}
