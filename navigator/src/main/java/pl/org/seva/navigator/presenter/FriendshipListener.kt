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
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import java.lang.ref.WeakReference
import java.util.Random

import javax.inject.Inject
import javax.inject.Singleton

import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.database.firebase.FirebaseWriter
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.ParcelableInt
import pl.org.seva.navigator.model.database.sqlite.SqlWriter
import pl.org.seva.navigator.view.builder.notification.PeerRequestedFriendshipNotificationBuilder

@Singleton
class FriendshipListener @Inject
internal constructor() {

    @Inject
    lateinit var contactsStore: ContactsStore
    @Inject
    lateinit var sqlWriter: SqlWriter
    @Inject
    lateinit var firebaseWriter: FirebaseWriter
    lateinit var acceptedReceiver: BroadcastReceiver
    lateinit var rejectedReceiver: BroadcastReceiver

    lateinit var weakContext: WeakReference<Context>

    fun init(context: Context) {
        this.weakContext = WeakReference(context)
    }

    fun onPeerRequestedFriendship(contact: Contact) {
        val context = weakContext.get() ?: return
        acceptedReceiver = FriendshipAcceptedBroadcastReceiver()
        rejectedReceiver = FriendshipRejectedBroadcastReceiver()
        context.registerReceiver(acceptedReceiver, IntentFilter(FRIENDSHIP_ACCEPTED_INTENT))
        context.registerReceiver(rejectedReceiver, IntentFilter(FRIENDSHIP_REJECTED_INTENT))
        val notificationId = ParcelableInt(Random().nextInt())
        val friendshipAccepted = Intent(FRIENDSHIP_ACCEPTED_INTENT)
                .putExtra(CONTACT_EXTRA, contact)
                .putExtra(NOTIFICATION_ID, notificationId)
        val yesPi = PendingIntent.getBroadcast(
                context,
                0,
                friendshipAccepted,
                PendingIntent.FLAG_UPDATE_CURRENT)
        val friendshipRejected = Intent(FRIENDSHIP_REJECTED_INTENT)
                .putExtra(CONTACT_EXTRA, contact)
                .putExtra(NOTIFICATION_ID, notificationId)
        val noPi = PendingIntent.getBroadcast(
                context,
                0,
                friendshipRejected,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = PeerRequestedFriendshipNotificationBuilder(context)
                .setContact(contact)
                .setNoPendingIntent(noPi)
                .setYesPendingIntent(yesPi)
                .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId.value, notification)
    }

    fun onPeerAcceptedFriendship(contact: Contact) {
        contactsStore.add(contact)
        sqlWriter.addFriend(contact)
        firebaseWriter.addFriendship(contact)
    }

    fun onPeerDeletedFriendship(contact: Contact) {
        contactsStore.delete(contact)
        sqlWriter.deleteFriend(contact)
    }

    private inner class FriendshipAcceptedBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val notificationId = intent.getParcelableExtra<ParcelableInt>(NOTIFICATION_ID).value
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.cancel(notificationId)
            context.unregisterReceiver(this)
            context.unregisterReceiver(rejectedReceiver)
            val contact = intent.getParcelableExtra<Contact>(CONTACT_EXTRA)
            if (contactsStore.contains(contact)) {
                return
            }
            contactsStore.add(contact)
            sqlWriter.addFriend(contact)
            firebaseWriter.acceptFriendship(contact)
        }
    }

    private inner class FriendshipRejectedBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val notificationId = intent.getParcelableExtra<ParcelableInt>(NOTIFICATION_ID).value
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.cancel(notificationId)
            context.unregisterReceiver(this)
            context.unregisterReceiver(acceptedReceiver)
        }
    }

    companion object {

        private val FRIENDSHIP_ACCEPTED_INTENT = "friendship_accepted_intent"
        private val FRIENDSHIP_REJECTED_INTENT = "friendship_rejected_intent"
        private val NOTIFICATION_ID = "notification_id"
        private val CONTACT_EXTRA = "contact_extra"
    }
}
