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
import android.content.Context
import com.google.android.gms.maps.model.LatLng
import pl.org.seva.navigator.model.Login
import pl.org.seva.navigator.model.firebase.FbWriter
import pl.org.seva.navigator.view.activity.NavigationActivity
import javax.inject.Inject

class NavigatorService : LifecycleService() {

    @javax.inject.Inject
    lateinit var activityRecognitionSource : pl.org.seva.navigator.source.ActivityRecognitionSource
    @javax.inject.Inject
    lateinit var myLocationSource : pl.org.seva.navigator.source.MyLocationSource
    @Inject
    lateinit var firebaseWriter: FbWriter
    @Inject
    lateinit var login: Login

    private val notificationBuilder by lazy { createNotificationBuilder() }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        (application as NavigatorApplication).component.inject(this)
        myLocationSource.init(this)
        startForeground(NavigatorService.Companion.ONGOING_NOTIFICATION_ID, createOngoingNotification())
        addMyLocationListener()

        return android.app.Service.START_STICKY
    }

    private fun addMyLocationListener() {
        myLocationSource.addLocationListener(lifecycle) { onLocationReceived(it) }
    }

    fun onLocationReceived(latLng: LatLng) {
        firebaseWriter.writeLocation(login.email!!, latLng)
    }

    private fun createOngoingNotification(): android.app.Notification {
        val mainActivityIntent = android.content.Intent(this, NavigationActivity::class.java)

        val pi = android.app.PendingIntent.getActivity(
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

    private fun createNotificationBuilder() : android.app.Notification.Builder {
        return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            (android.app.Notification.Builder(this))
        }
        else {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // The id of the channel.
            val id = "my_channel_01"
            // The user-visible name of the channel.
            val name = getString(R.string.channel_name)
            // The user-visible description of the channel.
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(id, name, importance)
            // Configure the notification channel.
            mChannel.description = description
            mNotificationManager.createNotificationChannel(mChannel)
            Notification.Builder(this, id)
        }
    }

    companion object {
        private val ONGOING_NOTIFICATION_ID = 1
    }
}