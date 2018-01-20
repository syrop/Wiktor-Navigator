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

package pl.org.seva.navigator.main.ui

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.NotificationChannels
import pl.org.seva.navigator.map.NavigationActivity

fun createNotificationBuilder(context: Context): Notification.Builder =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        else {
            Notification.Builder(context, NotificationChannels.QUESTION_CHANNEL_NAME)
        }

fun Context.createOngoingNotification(): Notification {
    val mainActivityIntent = android.content.Intent(this, NavigationActivity::class.java)

    val pi = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            mainActivityIntent,
            0)
    return createNotificationBuilder(this)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_navigation_white_24dp)
            .setContentIntent(pi)
            .setAutoCancel(false)
            .build()
}
