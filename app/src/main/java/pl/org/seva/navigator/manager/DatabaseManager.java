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

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.model.Contact;

public class DatabaseManager {

    private static final String USER_ROOT = "user";

    private static final String DISPLAY_NAME = "display_name";
    private static final String LAT_LNG = "lat_lng";
    private static final String FRIENDSHIP_REQUESTS = "friendship_requests";
    private static final String ACCEPTED_FRIENDSHIPS = "accepted_friendships";

    private static String to64(String str) {
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

    private static String from64(String str) {
        return new String(Base64.decode(str.getBytes(), Base64.NO_WRAP));
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

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();

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
    }

    public void login(FirebaseUser user) {
        String email64 = to64(user.getEmail());
        DatabaseReference userReference = database.getReference(USER_ROOT);
        userReference.setValue(email64);
        userReference = userReference.child(email64);
        userReference.child(DISPLAY_NAME).setValue(user.getDisplayName());
    }

    private Observable<DataSnapshot> readDataOnce(DatabaseReference reference) {
        ReplaySubject<DataSnapshot> result = ReplaySubject.create();
        reference.addListenerForSingleValueEvent(new ValueEventListener(result));

        return result.take(1);
    }

    public Observable<Contact> readContactOnceForEmail(String email) {
        return readDataOnce(emailReference(email))
                .map(DatabaseManager::snapshot2Contact);
    }

    private Observable<DataSnapshot> valueListener(DatabaseReference reference) {
        ReplaySubject<DataSnapshot> result = ReplaySubject.create();
        reference.addValueEventListener(new ValueEventListener(result));
        return result.hide();
    }

    private Observable<DataSnapshot> childListener(DatabaseReference reference) {
        ReplaySubject<DataSnapshot> result = ReplaySubject.create();
        reference.addChildEventListener(new ChildEventListener(result));
        return result.hide();
    }

    private static Contact snapshot2Contact(DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            return null;
        }
        Contact result = new Contact();
        result.setEmail(from64(snapshot.getKey()));
        result.setName((String) snapshot.child(DISPLAY_NAME).getValue());

        return result;
    }

    private DatabaseReference currentUserReference() {
        return emailReference(NavigatorApplication.email);
    }

    private DatabaseReference emailReference(String email) {
        return database.getReference(USER_ROOT + "/" + to64(email));
    }

    public void onLocationReceived(String email, LatLng latLng) {
        String email64 = to64(email);
        DatabaseReference ref = database.getReference(USER_ROOT).child(email64);
        ref.child(LAT_LNG).setValue(latLng2String(latLng));
    }

    public Observable<Contact> friendshipRequestedListener() {
        DatabaseReference reference = currentUserReference().child(FRIENDSHIP_REQUESTS);
        return readDataOnce(reference)
                .concatMapIterable(DataSnapshot::getChildren)
                .concatWith(childListener(reference))
                .map(DatabaseManager::snapshot2Contact);
    }

    public Observable<Contact> friendshipAcceptedListener() {
        DatabaseReference reference = currentUserReference().child(ACCEPTED_FRIENDSHIPS);
        return readDataOnce(reference)
                .concatMapIterable(DataSnapshot::getChildren)
                .concatWith(childListener(reference))
                .map(DatabaseManager::snapshot2Contact);
    }

    public void requestFriendship(Contact contact) {

    }

    private static class ValueEventListener implements com.google.firebase.database.ValueEventListener {

        private final ReplaySubject<DataSnapshot> subject;

        private ValueEventListener(ReplaySubject<DataSnapshot> subject) {
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

    private static class ChildEventListener implements com.google.firebase.database.ChildEventListener {

        private final ReplaySubject<DataSnapshot> subject;

        private ChildEventListener(ReplaySubject<DataSnapshot> subject) {
            this.subject = subject;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            subject.onNext(dataSnapshot);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            subject.onError(new Exception(databaseError.getMessage()));
        }
    }
}
