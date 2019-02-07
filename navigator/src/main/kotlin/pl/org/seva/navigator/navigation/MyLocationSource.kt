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

package pl.org.seva.navigator.navigation

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService

import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

import io.reactivex.subjects.PublishSubject
import pl.org.seva.navigator.main.ActivityRecognitionSource
import pl.org.seva.navigator.main.activityRecognition
import pl.org.seva.navigator.main.instance
import pl.org.seva.navigator.profile.isLoggedIn
import pl.org.seva.navigator.main.subscribe

val myLocationSource by instance<MyLocationSource>()

class MyLocationSource {

    private val provider by instance<FusedLocationProviderClient>()

    private val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_FREQUENCY)
            .setSmallestDisplacement(MIN_DISTANCE)

    private val callback = MyLocationCallback()

    private val locationSubject = PublishSubject.create<LatLng>()
    private val locationObservable = locationSubject
            .filter { isLoggedIn }
            .timestamp()
            .filter { it.time() - lastSentLocationTime >= UPDATE_FREQUENCY }
            .doOnNext { lastSentLocationTime = it.time() }
            .map { it.value() }

    private var location: Location? = null
    private var lastSentLocationTime: Long = 0
    private lateinit  var lifecycle: Lifecycle

    fun addLocationListener(lifecycle: Lifecycle, myLocationListener: (LatLng) -> Unit) =
        locationObservable.subscribe(lifecycle, myLocationListener)

    private fun addActivityRecognitionListeners() {
        activityRecognition.listen(lifecycle) { state ->
            when (state) {
                ActivityRecognitionSource.STATIONARY -> removeRequest()
                ActivityRecognitionSource.MOVING -> requestLocationUpdates()
            }
        }
    }

    infix fun withService(service: LifecycleService) {
        lifecycle = service.lifecycle
        requestLocationUpdates()
        addActivityRecognitionListeners()
    }

    fun onLocationChanged(location: Location) {
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
    private fun requestLocationUpdates() {
        provider.requestLocationUpdates(locationRequest, callback, null)
    }

    private fun removeRequest() {
        provider.removeLocationUpdates(callback)
    }

    inner class MyLocationCallback : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            onLocationChanged(result.lastLocation)
        }
    }

    companion object {

        private const val ACCURACY_THRESHOLD = 100.0  // [m]
        private const val UPDATE_FREQUENCY: Long = 30000  // [ms]

        /** Minimal distance (in meters) that will be counted between two subsequent updates.  */
        private const val MIN_DISTANCE = 5.0f

        private const val SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2

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
