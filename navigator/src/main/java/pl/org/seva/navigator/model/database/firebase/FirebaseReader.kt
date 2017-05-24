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

package pl.org.seva.navigator.model.database.firebase

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import pl.org.seva.navigator.model.Contact

@Singleton
class FirebaseReader @Inject
internal constructor() : FirebaseBase() {

    private fun createContactObservable(tag: String, delete: Boolean): Observable<Contact> {
        val reference = currentUserReference().child(tag)
        return readDataOnce(reference)
                .concatMapIterable<DataSnapshot> { it.children }
                .concatWith(childListener(reference))
                .doOnNext { snapshot -> if (delete) reference.child(snapshot.key).removeValue() }
                .map<Contact>({ snapshot2Contact(it) })
    }

    private fun readDataOnce(reference: DatabaseReference): Observable<DataSnapshot> {
        val resultSubject = PublishSubject.create<DataSnapshot>()
        return resultSubject
                .doOnSubscribe { _ ->
                    reference.addListenerForSingleValueEvent(RxValueEventListener(resultSubject))
                }
                .take(1)
    }

    private fun readData(reference: DatabaseReference): Observable<DataSnapshot> {
        val resultSubject = PublishSubject.create<DataSnapshot>()
        val `val` = RxValueEventListener(resultSubject)

        return resultSubject
                .doOnSubscribe { _ -> reference.addValueEventListener(`val`) }
                .doOnDispose { reference.removeEventListener(`val`) }
    }

    private fun childListener(reference: DatabaseReference): Observable<DataSnapshot> {
        val result = ReplaySubject.create<DataSnapshot>()
        reference.addChildEventListener(RxChildEventListener(result))
        return result.hide()
    }

    private fun snapshot2Contact(snapshot: DataSnapshot): Contact {
        val resultContact = Contact()
        if (!snapshot.exists()) {
            return resultContact
        }
        resultContact.setEmail(FirebaseBase.Companion.from64(snapshot.key))
        resultContact.setName(snapshot.child(FirebaseBase.Companion.DISPLAY_NAME).value as String)

        return resultContact
    }

    fun peerLocationListener(email: String): Observable<LatLng> {
        return readData(email2Reference(email).child(FirebaseBase.Companion.LAT_LNG))
                .map<Any> { it.value }
                .map { obj -> obj as String }
                .map<LatLng> { FirebaseBase.string2LatLng(it) }
    }

    fun friendshipRequestedListener(): Observable<Contact> {
        return createContactObservable(FirebaseBase.Companion.FRIENDSHIP_REQUESTED, true)
    }

    fun friendshipAcceptedListener(): Observable<Contact> {
        return createContactObservable(FirebaseBase.Companion.FRIENDSHIP_ACCEPTED, true)
    }

    fun friendshipDeletedListener(): Observable<Contact> {
        return createContactObservable(FirebaseBase.Companion.FRIENDSHIP_DELETED, true)
    }

    fun friendsListener(): Observable<Contact> {
        return createContactObservable(FirebaseBase.Companion.FRIENDS, false)
    }

    fun readContactOnceForEmail(email: String): Observable<Contact> {
        return readDataOnce(email2Reference(email))
                .map<Contact> { snapshot2Contact(it) }
    }
}
