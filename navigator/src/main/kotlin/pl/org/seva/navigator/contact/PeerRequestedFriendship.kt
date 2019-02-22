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

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ParcelableInt
import pl.org.seva.navigator.main.ui.createNotificationBuilder

inline fun friendshipRequestedNotification(context: Context, f: PeerRequestedFriendship.() -> Unit) =
        PeerRequestedFriendship(context).apply { f() }.build()

class PeerRequestedFriendship(private val context: Context) {

    lateinit var contact: Contact
    lateinit var nid: ParcelableInt

    fun build(): Notification {
        fun Int.pi(): PendingIntent {
            val intent = Intent(FriendshipListener.FRIENDSHIP_REQUESTED_INTENT)
                    .putExtra(FriendshipListener.CONTACT_EXTRA, contact)
                    .putExtra(FriendshipListener.NOTIFICATION_ID, nid)
                    .putExtra(FriendshipListener.ACTION, ParcelableInt(this))
            return PendingIntent.getBroadcast(context, this, intent, PI_FLAG)
        }

        val message = context.resources
                .getString(R.string.friendship_confirmation)
                .replace(NAME_TAG, contact.name)
                .replace(EMAIL_TAG, contact.email)

        // http://stackoverflow.com/questions/6357450/android-multiline-notifications-notifications-with-longer-text#22964072
        val bigTextStyle = Notification.BigTextStyle()
        bigTextStyle.setBigContentTitle(context.getString(R.string.app_name))
        bigTextStyle.bigText(message)

        // http://stackoverflow.com/questions/11883534/how-to-dismiss-notification-after-action-has-been-clicked#11884313
        return createNotificationBuilder(context)
                .setStyle(bigTextStyle)
                .setContentText(context.getText(R.string.friendship_requested_notification_short))
                .setSmallIcon(R.drawable.notification)
                .setAutoCancel(false)
                .addAction(
                        R.drawable.ic_close_black_24dp,
                        context.getString(android.R.string.no),
                        FriendshipListener.REJECTED_ACTION.pi())
                .addAction(
                        R.drawable.ic_check_black_24dp,
                        context.getString(android.R.string.yes),
                        FriendshipListener.ACCEPTED_ACTION.pi())
                .build()
    }

    companion object {
        private const val NAME_TAG = "[name]"
        private const val EMAIL_TAG = "[email]"
        private const val PI_FLAG = PendingIntent.FLAG_ONE_SHOT
    }
}
