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

    private Observable<Contact> createContactObservable(String tag, boolean delete) {
        DatabaseReference reference = currentUserReference().child(tag);
        return readDataOnce(reference)
                .concatMapIterable(DataSnapshot::getChildren)
                .concatWith(childListener(reference))
                .doOnNext(snapshot -> { if (delete) reference.child(snapshot.getKey()).removeValue();} )
                .map(FirebaseReader::snapshot2Contact);
    }

    private Observable<DataSnapshot> readDataOnce(DatabaseReference reference) {
        PublishSubject<DataSnapshot> resultSubject = PublishSubject.create();
        return resultSubject
                .doOnSubscribe(__ -> reference.addListenerForSingleValueEvent(new RxValueEventListener(resultSubject)))
                .take(1);
    }

    private Observable<DataSnapshot> readData(DatabaseReference reference) {
        PublishSubject<DataSnapshot> resultSubject = PublishSubject.create();
        ValueEventListener val = new RxValueEventListener(resultSubject);

        return resultSubject
                .doOnSubscribe(__ -> reference.addValueEventListener(val))
                .doOnDispose(() -> reference.removeEventListener(val));
    }

    private Observable<DataSnapshot> childListener(DatabaseReference reference) {
        ReplaySubject<DataSnapshot> result = ReplaySubject.create();
        reference.addChildEventListener(new RxChildEventListener(result));
        return result.hide();
    }

    private static Contact snapshot2Contact(DataSnapshot snapshot) {
        Contact resultContact = new Contact();
        if (!snapshot.exists()) {
            return resultContact;
        }
        resultContact.setEmail(from64(snapshot.getKey()));
        resultContact.setName((String) snapshot.child(DISPLAY_NAME).getValue());

        return resultContact;
    }

    public Observable<LatLng> peerLocationListener(String email) {
        return readData(email2Reference(email).child(LAT_LNG))
                .map(DataSnapshot::getValue)
                .map(obj -> (String) obj)
                .map(FirebaseBase::string2LatLng);
    }

    public Observable<Contact> friendshipRequestedListener() {
        return createContactObservable(FRIENDSHIP_REQUESTED, true);
    }

    public Observable<Contact> friendshipAcceptedListener() {
        return createContactObservable(FRIENDSHIP_ACCEPTED, true);
    }

    public Observable<Contact> friendshipDeletedListener() {
        return createContactObservable(FRIENDSHIP_DELETED, true);
    }

    public Observable<Contact> friendsListener() {
        return createContactObservable(FRIENDS, false);
    }

    public Observable<Contact> readContactOnceForEmail(String email) {
        return readDataOnce(email2Reference(email))
                .map(FirebaseReader::snapshot2Contact);
    }
}
