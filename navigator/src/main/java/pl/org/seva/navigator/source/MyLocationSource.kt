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

import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleService
import android.location.Location
import android.os.Bundle
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import pl.org.seva.navigator.model.Login

class MyLocationSource : LiveSource(),
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    com.google.android.gms.location.LocationListener,
    KodeinGlobalAware {

    private val activityRecognitionSource: ActivityRecognitionSource = instance()
    private val login: Login = instance()

    private var googleApiClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null

    private val locationSubject: PublishSubject<LatLng> = PublishSubject.create<LatLng>()
    private val locationObservable: Observable<LatLng>

    private var location: Location? = null
    private var lastSentLocationTime: Long = 0
    private lateinit  var lifecycle: Lifecycle

    init {
        locationObservable = locationSubject
                .filter { login.isLoggedIn }
                .timestamp()
                .filter { it.time() - lastSentLocationTime >= UPDATE_FREQUENCY }
                .doOnNext { lastSentLocationTime = it.time() }
                .map { it.value() }
    }

    fun addLocationListener(lifecycle: Lifecycle, myLocationListener: (LatLng) -> Unit) =
            lifecycle.observe { locationObservable.subscribe(myLocationListener) }

    private fun addActivityRecognitionListeners() =
            activityRecognitionSource.addActivityRecognitionListener(lifecycle,
                    onStationary = { removeRequest() },
                    onMoving = { request() })

    private fun connectGoogleApiClient() = googleApiClient!!.connect()

    fun init(service: LifecycleService) {
        lifecycle = service.lifecycle
        googleApiClient?:let {
            googleApiClient = GoogleApiClient.Builder(service)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build()
        }

        connectGoogleApiClient()
    }

    override fun onLocationChanged(location: Location) {
        if (location.accuracy >= ACCURACY_THRESHOLD) {
            return
        }
        if (!isBetterLocation(location, this.location)) {
            return
        }
        this.location = location
        locationSubject.onNext(LatLng(location.latitude, location.longitude))
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        location?:let {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_FREQUENCY)
                .setSmallestDisplacement(MIN_DISTANCE)
        request()
        addActivityRecognitionListeners()
    }

    @SuppressLint("MissingPermission")
    private fun request() {
        if (googleApiClient == null || locationRequest == null) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    private fun removeRequest() {
        googleApiClient?.apply {
            LocationServices.FusedLocationApi.removeLocationUpdates(this, this@MyLocationSource) }
    }

    override fun onConnectionSuspended(i: Int) = Unit

    override fun onConnectionFailed(connectionResult: ConnectionResult) = Unit

    companion object {

        private val ACCURACY_THRESHOLD = 100.0  // [m]
        private val UPDATE_FREQUENCY: Long = 30000  // [ms]

        /** Minimal distance (in meters) that will be counted between two subsequent updates.  */
        private val MIN_DISTANCE = 5.0f

        private val SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2

        private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
            currentBestLocation?: return true // A new location is always better than no location

            // Check whether the new location fix is newer or older
            val timeDelta = location.time - currentBestLocation.time
            val isSignificantlyNewer = timeDelta > SIGNIFICANT_TIME_LAPSE
            val isSignificantlyOlder = timeDelta < -SIGNIFICANT_TIME_LAPSE
            val isNewer = timeDelta > 0

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                return true
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                return false
            }

            // Check whether the new location fix is more or less accurate
            val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
            val isMoreAccurate = accuracyDelta < 0
            val isSignificantlyLessAccurate = accuracyDelta > 200

            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate) {
                return true
            } else if (isNewer && !isSignificantlyLessAccurate) {
                return true
            }
            return false
        }
    }
}
