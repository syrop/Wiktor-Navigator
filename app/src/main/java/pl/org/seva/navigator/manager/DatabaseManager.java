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

package pl.org.seva.navigator.manager;

import android.util.Base64;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import rx.Observable;
import rx.subjects.PublishSubject;

public class DatabaseManager {

    private static final String USER = "user";
    private static final String UID = "uid";
    private static final String DISPLAY_NAME = "display_name";
    private static final String LAT_LNG = "lat_lng";

    private static String to64(String str) {
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

    public static String latLng2String(LatLng latLng) {
        return Double.toString(latLng.latitude) + ";" + latLng.longitude;
    }

    public static LatLng string2LatLng(String str) {
        int semicolon = str.indexOf(';');
        double lat = Double.parseDouble(str.substring(0, semicolon));
        double lon = Double.parseDouble(str.substring(semicolon + 1));
        return new LatLng(lat, lon);
    }

    private static DatabaseManager instance;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private DatabaseManager() {
        //
    }

    public void login(FirebaseUser user) {
        String email64 = to64(user.getEmail());
        DatabaseReference userReference = database.getReference(USER);
        userReference.setValue(email64);
        userReference = userReference.child(email64);
        userReference.child(UID).setValue(user.getUid());
        userReference.child(DISPLAY_NAME).setValue(to64(user.getDisplayName()));
    }

    private Observable<DataSnapshot> readDataOnce(DatabaseReference reference) {
        PublishSubject<DataSnapshot> result = PublishSubject.create();
        reference.addListenerForSingleValueEvent(new ValueEventListener(result));

        return result.take(1);
    }

    public Observable<DataSnapshot> readData(DatabaseReference reference) {
        PublishSubject<DataSnapshot> result = PublishSubject.create();
        reference.addListenerForSingleValueEvent(new ValueEventListener(result));

        return result.asObservable();
    }

    private Observable<Boolean> doesExist(DatabaseReference reference) {
        return readDataOnce(reference).map(DataSnapshot::exists);
    }

    public Observable<Boolean> doesEmailExist(String email) {
        return doesExist(database.getReference(USER + "/" + to64(email)));
    }

    public void onLocationReceived(String email, LatLng latLng) {
        String email64 = to64(email);
        DatabaseReference ref = database.getReference(USER).child(email64);
        ref.child(LAT_LNG).setValue(latLng2String(latLng));
    }

    private static class ValueEventListener implements com.google.firebase.database.ValueEventListener {

        private PublishSubject<DataSnapshot> subject;

        private ValueEventListener(PublishSubject<DataSnapshot> subject) {
            this.subject = subject;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            subject.onNext(dataSnapshot);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            subject.onError(new Exception(databaseError.getMessage()));
        }
    }
}
