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

package pl.org.seva.navigator.model.database.firebase;

import android.util.Base64;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

import pl.org.seva.navigator.NavigatorApplication;

public class FirebaseBase {

    static final String USER_ROOT = "user";
    static final String DISPLAY_NAME = "display_name";
    static final String LAT_LNG = "lat_lng";
    static final String FRIENDSHIP_REQUESTED = "friendship_requested";
    static final String FRIENDSHIP_ACCEPTED = "friendship_accepted";
    static final String FRIENDSHIP_DELETED = "friendship_deleted";
    static final String FRIENDS = "friends";

    final FirebaseDatabase database = FirebaseDatabase.getInstance();

    static String to64(String str) {
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

    static String from64(String str) {
        return new String(Base64.decode(str.getBytes(), Base64.NO_WRAP));
    }

    public static String latLng2String(LatLng latLng) {
        return String.format(Locale.US, "%.3f", latLng.latitude) + ";" +
                String.format(Locale.US, "%.3f", latLng.longitude);
    }

    public static LatLng string2LatLng(String str) {
        int semicolon = str.indexOf(';');
        double lat = Double.parseDouble(str.substring(0, semicolon));
        double lon = Double.parseDouble(str.substring(semicolon + 1));
        return new LatLng(lat, lon);
    }

    DatabaseReference currentUserReference() {
        return email2Reference(NavigatorApplication.email);
    }

    DatabaseReference email2Reference(String email) {
        String referencePath = USER_ROOT + "/" + to64(email);
        return database.getReference(referencePath);
    }
}
