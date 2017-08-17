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

package pl.org.seva.navigator

import android.app.*
import android.arch.lifecycle.LifecycleService
import android.os.Build
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import com.google.android.gms.maps.model.LatLng
import pl.org.seva.navigator.model.Login
import pl.org.seva.navigator.model.firebase.FbWriter
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.view.activity.NavigationActivity
import pl.org.seva.navigator.view.builder.notification.Channels

class NavigatorService: LifecycleService(), KodeinGlobalAware {

    private val myLocationSource: MyLocationSource = instance()
    private val firebaseWriter: FbWriter = instance()
    private val login: Login = instance()

    private val notificationBuilder by lazy { createNotificationBuilder() }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        myLocationSource.init(this)
        startForeground(NavigatorService.Companion.ONGOING_NOTIFICATION_ID, createOngoingNotification())
        addMyLocationListener()

        return android.app.Service.START_STICKY
    }

    private fun addMyLocationListener() =
            myLocationSource.addLocationListener(lifecycle) { onLocationReceived(it) }

    private fun onLocationReceived(latLng: LatLng) = firebaseWriter.writeLocation(login.email!!, latLng)

    private fun createOngoingNotification(): Notification {
        val mainActivityIntent = android.content.Intent(this, NavigationActivity::class.java)

        val pi = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                mainActivityIntent,
                0)
        return notificationBuilder
                .setContentTitle(getString(pl.org.seva.navigator.R.string.app_name))
                .setSmallIcon(pl.org.seva.navigator.R.drawable.ic_navigation_white_24dp)
                .setContentIntent(pi)
                .setAutoCancel(false)
                .build()
    }

    private fun createNotificationBuilder(): Notification.Builder =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                (Notification.Builder(this))
            }
            else {
                Notification.Builder(this, Channels.ONGOING_NOTIFICATION_CHANNEL_NAME)
            }

    companion object {
        private val ONGOING_NOTIFICATION_ID = 1
    }
}
