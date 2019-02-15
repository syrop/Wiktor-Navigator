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

package pl.org.seva.navigator.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.appContext
import pl.org.seva.navigator.main.instance

val notificationChannels by instance<NotificationChannels>()

fun createNotificationChannels() = notificationChannels.create()

class NotificationChannels {

    fun create() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createOngoingChannel(nm)
        createQuestionChannel(nm)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createOngoingChannel(nm: NotificationManager) {
        val id = ONGOING_NOTIFICATION_CHANNEL_NAME
        // The user-visible name of the channel.
        val name = appContext.getString(R.string.question_channel_name)
        // The user-visible description of the channel.
        val description = appContext.getString(R.string.question_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        nm.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createQuestionChannel(nm: NotificationManager) {
        val id = QUESTION_CHANNEL_NAME
        // The user-visible name of the channel.
        val name = appContext.getString(R.string.ongoing_channel_name)
        // The user-visible description of the channel.
        val description = appContext.getString(R.string.ongoing_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        channel.setShowBadge(true)
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val ONGOING_NOTIFICATION_CHANNEL_NAME = "my_channel_01"
        const val QUESTION_CHANNEL_NAME = "question_channel"
    }
}
