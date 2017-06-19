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
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.google.android.gms.maps.model.LatLng
import pl.org.seva.navigator.NavigatorApplication
import pl.org.seva.navigator.R
import pl.org.seva.navigator.model.database.firebase.FirebaseWriter
import pl.org.seva.navigator.source.ActivityRecognitionSource
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.view.activity.NavigationActivity
import javax.inject.Inject

class NavigatorService : android.arch.lifecycle.LifecycleService() {

    @javax.inject.Inject
    lateinit var activityRecognitionSource : pl.org.seva.navigator.source.ActivityRecognitionSource
    @javax.inject.Inject
    lateinit var myLocationSource : pl.org.seva.navigator.source.MyLocationSource
    @Inject
    lateinit var firebaseWriter: FirebaseWriter

    private val notificationBuilder by lazy { createNotificationBuilder() }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        (application as pl.org.seva.navigator.NavigatorApplication).component.inject(this)
        startForeground(pl.org.seva.navigator.NavigatorService.Companion.ONGOING_NOTIFICATION_ID, createOngoingNotification())
        addActivityRecognitionListeners()
        addMyLocationListener()

        return android.app.Service.START_STICKY
    }

    private fun addActivityRecognitionListeners() {
        activityRecognitionSource.addActivityRecognitionListener(
                lifecycle,
                stationaryListener = { onDeviceStationary() },
                movingListener = { onDeviceMoving() })
    }

    private fun addMyLocationListener() {
        myLocationSource.addLocationListener { onLocationReceived(it) }
    }

    private fun onDeviceStationary() {
        myLocationSource.paused = true
        myLocationSource.removeRequest()
    }

    private fun onDeviceMoving() {
        myLocationSource.paused = false
        myLocationSource.request()
    }

    fun onLocationReceived(latLng: LatLng) {
        firebaseWriter.writeMyLocation(NavigatorApplication.email!!, latLng)
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