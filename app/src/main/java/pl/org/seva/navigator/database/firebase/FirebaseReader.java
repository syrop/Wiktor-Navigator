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

package pl.org.seva.navigator.database.firebase;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import pl.org.seva.navigator.model.Contact;

@Singleton
public class FirebaseReader extends FirebaseBase {

    @Inject
    FirebaseReader() {
        super();
    }

    public Observable<LatLng> peerLocationListener(String email) {
        return readData(email2Reference(email).child(LAT_LNG))
                .map(DataSnapshot::getValue)
                .map(obj -> (String) obj)
                .map(FirebaseBase::string2LatLng);
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
                .map(FirebaseReader::snapshot2Contact);
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
                .map(FirebaseReader::snapshot2Contact);
    }

    private Observable<DataSnapshot> readDataOnce(DatabaseReference reference) {
        PublishSubject<DataSnapshot> result = PublishSubject.create();
        return result
                .doOnSubscribe(__ -> reference.addListenerForSingleValueEvent(new RxValueEventListener(result)))
                .take(1);
    }

    private Observable<DataSnapshot> readData(DatabaseReference reference) {
        PublishSubject<DataSnapshot> result = PublishSubject.create();
        ValueEventListener val = new RxValueEventListener(result);

        return result
                .doOnSubscribe(__ -> reference.addValueEventListener(val))
                .doOnDispose(() -> reference.removeEventListener(val));
    }

    public Observable<Contact> readContactOnceForEmail(String email) {
        return readDataOnce(email2Reference(email))
                .map(FirebaseReader::snapshot2Contact);
    }

    private Observable<DataSnapshot> childListener(DatabaseReference reference) {
        ReplaySubject<DataSnapshot> result = ReplaySubject.create();
        reference.addChildEventListener(new RxChildEventListener(result));
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
}
