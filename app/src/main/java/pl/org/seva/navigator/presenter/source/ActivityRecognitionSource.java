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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.navigator.presenter.receiver.ActivityRecognitionReceiver;

@Singleton
public class ActivityRecognitionSource implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String ACTIVITY_RECOGNITION = "activity_recognition_intent";

    private static final long ACTIVITY_RECOGNITION_INTERVAL = 1000;  // [ms]

    private static final PublishSubject<Object> stationarySubject = PublishSubject.create();
    private static final PublishSubject<Object> movingSubject = PublishSubject.create();

    private boolean initialized;
    private GoogleApiClient googleApiClient;
    private WeakReference<Context> weakContext;

    @Inject
    ActivityRecognitionSource() {
    }

    public void init(Context context) {
        if (initialized) {
            return;
        }
        weakContext = new WeakReference<>(context);
        context.registerReceiver(new ActivityRecognitionBroadcastReceiver(), new IntentFilter(ACTIVITY_RECOGNITION));
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(com.google.android.gms.location.ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            googleApiClient.connect();
        }

        initialized = true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Context context = weakContext.get();
        if (context == null) {
            return;
        }
        Intent intent = new Intent(ACTIVITY_RECOGNITION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        com.google.android.gms.location.ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                googleApiClient,
                ACTIVITY_RECOGNITION_INTERVAL,
                pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    void addActivityRecognitionReceiver(ActivityRecognitionReceiver activityRecognitionReceiver) {
        stationarySubject.subscribe(__ -> activityRecognitionReceiver.onDeviceStationary());
        movingSubject.subscribe(__ -> activityRecognitionReceiver.onDeviceMoving());
    }

    private class ActivityRecognitionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                if (result.getMostProbableActivity().getType() == DetectedActivity.STILL) {
                    stationarySubject.onNext(0);
                }
                else {
                    movingSubject.onNext(0);
                }
            }
        }
    }
}
