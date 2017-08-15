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

package pl.org.seva.navigator.view.builder.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

import pl.org.seva.navigator.R
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.ParcelableInt
import pl.org.seva.navigator.presenter.FriendshipListener

fun friendshipRequestedNotification(context: Context, f: PeerRequestedFriendship.() -> Unit) =
        PeerRequestedFriendship(context).apply { f() }.build()

class PeerRequestedFriendship(private val context: Context) {
    lateinit var contact: Contact
    lateinit var notificationId: ParcelableInt

    fun build(): Notification {
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
                .setSmallIcon(R.drawable.ic_navigation_white_24dp)
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

    private fun Int.pi(): PendingIntent {
        val intent = Intent(FriendshipListener.FRIENDSHIP_REQUESTED_INTENT)
                .putExtra(FriendshipListener.CONTACT_EXTRA, contact)
                .putExtra(FriendshipListener.NOTIFICATION_ID, notificationId)
                .putExtra(FriendshipListener.ACTION, ParcelableInt(this))
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT)
    }

    private fun createNotificationBuilder(context: Context): Notification.Builder {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        else {
            Notification.Builder(context, Channels.QUESTION_CHANNEL_NAME)
        }
    }

    companion object {
        private val NAME_TAG = "[name]"
        private val EMAIL_TAG = "[email]"
    }
}
