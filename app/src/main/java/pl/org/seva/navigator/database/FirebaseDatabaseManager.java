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

package pl.org.seva.navigator.database;

import android.util.Base64;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.model.Contact;

@Singleton
public class FirebaseDatabaseManager {

    private static final String TAG = FirebaseDatabaseManager.class.getSimpleName();

    private static final String USER_ROOT = "user";

    private static final String DISPLAY_NAME = "display_name";
    private static final String LAT_LNG = "lat_lng";
    private static final String FRIENDSHIP_REQUESTS = "friendship_requests";
    private static final String FRIENDSHIP_ACCEPTED = "friendship_accepted";
    private static final String FRIENDSHIP_DELETED = "friendship_deleted";

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

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();

    @Inject public FirebaseDatabaseManager() {
        //
    }

    public void login(FirebaseUser user) {
        Contact contact = new Contact()
                .setEmail(user.getEmail())
                .setName(user.getDisplayName());
        writeContact(database.getReference(USER_ROOT), contact);
    }

    private void writeContact(DatabaseReference reference, Contact contact) {
        String email64 = to64(contact.email());
        reference = reference.child(email64);
        reference.child(DISPLAY_NAME).setValue(contact.name());
    }

    private Observable<DataSnapshot> readDataOnce(DatabaseReference reference) {
        ReplaySubject<DataSnapshot> result = ReplaySubject.create();
        reference.addListenerForSingleValueEvent(new ValueEventListener(result));

        return result.take(1);
    }

    public Observable<Contact> readContactOnceForEmail(String email) {
        return readDataOnce(email2Reference(email))
                .map(FirebaseDatabaseManager::snapshot2Contact);
    }

    private Observable<DataSnapshot> childListener(DatabaseReference reference) {
        ReplaySubject<DataSnapshot> result = ReplaySubject.create();
        reference.addChildEventListener(new ChildEventListener(result));
        return result.hide();
    }

    private static Contact snapshot2Contact(DataSnapshot snapshot) {
        Contact result = new Contact();
        if (!snapshot.exists()) {
            return result;
        }
        result.setEmail(from64(snapshot.getKey()));
        result.setName((String) snapshot.child(DISPLAY_NAME).getValue());

        return result;
    }

    private DatabaseReference currentUserReference() {
        return email2Reference(NavigatorApplication.email);
    }

    private DatabaseReference email2Reference(String email) {
        String referencePath = USER_ROOT + "/" + to64(email);
        Log.d(TAG, "Reference: " + referencePath);
        return database.getReference(referencePath);
    }

    public void onMyLocationReceived(String email, LatLng latLng) {
        String email64 = to64(email);
        DatabaseReference ref = database.getReference(USER_ROOT).child(email64);
        ref.child(LAT_LNG).setValue(latLng2String(latLng));
    }

    public Observable<Contact> friendshipRequestedListener() {
        return createContactObservable(FRIENDSHIP_REQUESTS);
    }

    public Observable<Contact> friendshipAcceptedListener() {
        DatabaseReference reference = currentUserReference().child(FRIENDSHIP_ACCEPTED);
        return readDataOnce(reference)
                .concatMapIterable(DataSnapshot::getChildren)
                .concatWith(childListener(reference))
                .doOnNext(snapshot -> reference.child(snapshot.getKey()).removeValue())
                .map(FirebaseDatabaseManager::snapshot2Contact);
    }

    public Observable<Contact> friendshipDeletedListener() {
        return createContactObservable(FRIENDSHIP_DELETED);
    }

    private Observable<Contact> createContactObservable(String tag) {
        DatabaseReference reference = currentUserReference().child(tag);
        return readDataOnce(reference)
                .concatMapIterable(DataSnapshot::getChildren)
                .concatWith(childListener(reference))
                .doOnNext(snapshot -> reference.child(snapshot.getKey()).removeValue())
                .map(FirebaseDatabaseManager::snapshot2Contact);
    }

    public void requestFriendship(Contact contact) {
        DatabaseReference reference = email2Reference(contact.email()).child(FRIENDSHIP_REQUESTS);
        writeContact(reference, NavigatorApplication.getLoggedInContact());
    }

    public void acceptFriendship(Contact contact) {
        DatabaseReference reference = email2Reference(contact.email()).child(FRIENDSHIP_ACCEPTED);
        writeContact(reference, NavigatorApplication.getLoggedInContact());
    }

    public void deleteFriendsdhip(Contact contact) {
        DatabaseReference reference = email2Reference(contact.email()).child(FRIENDSHIP_ACCEPTED);
        writeContact(reference, NavigatorApplication.getLoggedInContact());
    }

    private static class ValueEventListener implements com.google.firebase.database.ValueEventListener {

        private final ReplaySubject<DataSnapshot> valueEventSubject;

        private ValueEventListener(ReplaySubject<DataSnapshot> subject) {
            this.valueEventSubject = subject;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            valueEventSubject.onNext(dataSnapshot);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            valueEventSubject.onError(new Exception(databaseError.getMessage()));
        }
    }

    private static class ChildEventListener implements com.google.firebase.database.ChildEventListener {

        private final ReplaySubject<DataSnapshot> childEventSubject;

        private ChildEventListener(ReplaySubject<DataSnapshot> subject) {
            this.childEventSubject = subject;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            childEventSubject.onNext(dataSnapshot);
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
            childEventSubject.onError(new Exception(databaseError.getMessage()));
        }
    }
}
