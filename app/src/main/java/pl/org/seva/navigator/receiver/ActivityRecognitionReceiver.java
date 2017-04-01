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

package pl.org.seva.navigator.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import javax.inject.Inject;

import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.source.ActivityRecognitionSource;

// https://shashikawlp.wordpress.com/2013/05/08/android-jelly-bean-notifications-with-actions/
public class ActivityRecognitionReceiver extends BroadcastReceiver {

    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject ActivityRecognitionSource activityRecognitionSource;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((NavigatorApplication) context.getApplicationContext()).getGraph().inject(this);
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (result.getMostProbableActivity().getType() == DetectedActivity.STILL) {
                onDeviceStationary();
            }
            else {
                onDeviceMoving();
            }
        }
    }

    private void onDeviceStationary() {
        activityRecognitionSource.onDeviceStationary();
    }

    private void onDeviceMoving() {
        activityRecognitionSource.onDeviceMoving();
    }
}
