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

package pl.org.seva.navigator.source

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

import java.lang.ref.WeakReference

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.subjects.PublishSubject
import pl.org.seva.navigator.presenter.ActivityRecognitionListener

@Singleton
class ActivityRecognitionSource @Inject
internal constructor() : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private var initialized: Boolean = false
    private var googleApiClient: GoogleApiClient? = null
    lateinit var weakContext: WeakReference<Context>

    fun init(context: Context) {
        if (initialized) {
            return
        }
        weakContext = WeakReference(context)
        context.registerReceiver(
                ActivityRecognitionBroadcastReceiver(),
                IntentFilter(ACTIVITY_RECOGNITION_INTENT))
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(context)
                    .addApi(com.google.android.gms.location.ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build()
            googleApiClient!!.connect()
        }

        initialized = true
    }

    override fun onConnected(bundle: Bundle?) {
        val context = weakContext.get() ?: return
        val intent = Intent(ACTIVITY_RECOGNITION_INTENT)
        val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                googleApiClient,
                ACTIVITY_RECOGNITION_INTERVAL,
                pendingIntent)
    }

    override fun onConnectionSuspended(i: Int) {

    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    fun addActivityRecognitionListener(activityRecognitionListener: ActivityRecognitionListener) {
        stationarySubject.subscribe { _ -> activityRecognitionListener.onDeviceStationary() }
        movingSubject.subscribe { _ -> activityRecognitionListener.onDeviceMoving() }
    }

    private inner class ActivityRecognitionBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)
                if (result.mostProbableActivity.type == DetectedActivity.STILL) {
                    onDeviceStationary()
                } else {
                    onDeviceMoving()
                }
            }
        }
    }

    private fun onDeviceStationary() {
        stationarySubject.onNext(0)
    }

    private fun onDeviceMoving() {
        movingSubject.onNext(0)
    }

    companion object {

        private val ACTIVITY_RECOGNITION_INTENT = "activity_recognition_intent"

        private val ACTIVITY_RECOGNITION_INTERVAL: Long = 1000  // [ms]

        private val stationarySubject = PublishSubject.create<Any>()
        private val movingSubject = PublishSubject.create<Any>()
    }
}
