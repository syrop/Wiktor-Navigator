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

import com.google.android.gms.maps.model.LatLng;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.database.firebase.FirebaseWriter;

@Singleton
public class MyLocationReceiver {

    @SuppressWarnings("WeakerAccess")
    @Inject
    FirebaseWriter firebaseWriter;

    @Inject
    MyLocationReceiver() {
    }

    public void onLocationReceived(LatLng latLng) {
        firebaseWriter.storeMyLocation(NavigatorApplication.email, latLng);
    }
}
