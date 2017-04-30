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

package pl.org.seva.navigator.presenter.source;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.schedulers.Timed;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.navigator.NavigatorApplication;
import pl.org.seva.navigator.presenter.listener.ActivityRecognitionListener;
import pl.org.seva.navigator.presenter.listener.MyLocationListener;

@SuppressWarnings("MissingPermission")
@Singleton
public class MyLocationSource implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        ActivityRecognitionListener {

    @SuppressWarnings("WeakerAccess")
    @Inject ActivityRecognitionSource activityRecognitionSource;

    private static final String TAG = MyLocationSource.class.getSimpleName();

    private static final double ACCURACY_THRESHOLD = 100.0;  // [m]
    private static final long UPDATE_FREQUENCY = 30000;  // [ms]

    /** Minimal distance (in meters) that will be counted between two subsequent updates. */
    private static final float MIN_DISTANCE = 5.0f;

    private static final int SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private final PublishSubject<LatLng> locationSubject;
    private final Observable<LatLng> locationObservable;

    /** Location last received from the update. */
    private Location location;

    private boolean paused;

    @SuppressWarnings("WeakerAccess")
    @Inject
    MyLocationSource() {
        locationSubject = PublishSubject.create();

        locationObservable = locationSubject
                .filter(latLng -> NavigatorApplication.isLoggedIn)
                .timeInterval()
                .filter(a -> a.time() >= UPDATE_FREQUENCY)
                .map(Timed::value);
    }

    public void addLocationListener(MyLocationListener myLocationListener) {
        locationObservable.subscribe(myLocationListener::onLocationReceived);
    }

    public void connectGoogleApiClient() {
        googleApiClient.connect();
    }

    public MyLocationSource init(Context context) {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        return this;
    }

    private static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > SIGNIFICANT_TIME_LAPSE;
        boolean isSignificantlyOlder = timeDelta < -SIGNIFICANT_TIME_LAPSE;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        }
        else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        }
        else if (isNewer && !isSignificantlyLessAccurate) {
            return true;
        }
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getAccuracy() >= ACCURACY_THRESHOLD) {
            return;
        }
        if (!isBetterLocation(location, this.location)) {
            return;
        }
        this.location = location;
        locationSubject.onNext(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (location == null) {
            //noinspection MissingPermission
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_FREQUENCY)
                .setSmallestDisplacement(MIN_DISTANCE);

        activityRecognitionSource.addActivityRecognitionListener(this);

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        LocationServices.FusedLocationApi.
                requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onDeviceStationary() {
        pauseUpdates();
    }

    private void pauseUpdates() {
        if (paused) {
            return;
        }
        Log.d(TAG, "Pause updates.");
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);

        paused = true;
    }

    @Override
    public void onDeviceMoving() {
        resumeUpdates();
    }

    private void resumeUpdates() {
        if (!paused) {
            return;
        }
        Log.d(TAG, "Resume updates.");
        requestLocationUpdates();

        paused = false;
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
}
