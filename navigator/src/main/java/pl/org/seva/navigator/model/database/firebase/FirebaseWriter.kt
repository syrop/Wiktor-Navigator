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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference

import javax.inject.Inject
import javax.inject.Singleton

import pl.org.seva.navigator.NavigatorApplication
import pl.org.seva.navigator.model.Contact

@Singleton
class FirebaseWriter @Inject
internal constructor() : FirebaseBase() {

    fun login(user: FirebaseUser) {
        val contact = Contact(user.email!!, user.displayName!!)
        writeContact(database.getReference(FirebaseBase.Companion.USER_ROOT), contact)
    }

    private fun writeContact(reference: DatabaseReference, contact: Contact) {
        val email64 = FirebaseBase.Companion.to64(contact.email!!)
        val localReference = reference.child(email64)
        localReference.child(FirebaseBase.Companion.DISPLAY_NAME).setValue(contact.name!!)
    }

    fun writeMyLocation(email: String, latLng: LatLng) {
        email2Reference(email).child(FirebaseBase.Companion.LAT_LNG)
                .setValue(FirebaseBase.Companion.latLng2String(latLng))
    }

    fun requestFriendship(contact: Contact) {
        val reference = email2Reference(contact.email!!).child(FirebaseBase.Companion.FRIENDSHIP_REQUESTED)
        writeContact(reference, NavigatorApplication.loggedInContact)
    }

    fun acceptFriendship(contact: Contact) {
        val reference = email2Reference(contact.email!!).child(FirebaseBase.Companion.FRIENDSHIP_ACCEPTED)
        writeContact(reference, NavigatorApplication.loggedInContact)
        addFriendship(contact)
    }

    fun addFriendship(contact: Contact) {
        val reference = email2Reference(NavigatorApplication.loggedInContact.email!!)
                .child(FirebaseBase.Companion.FRIENDS)
        writeContact(reference, contact)
    }

    fun deleteFriendship(contact: Contact) {
        var reference = email2Reference(contact.email!!).child(FirebaseBase.Companion.FRIENDSHIP_DELETED)
        writeContact(reference, NavigatorApplication.loggedInContact)
        reference = email2Reference(NavigatorApplication.loggedInContact.email!!)
                .child(FirebaseBase.Companion.FRIENDS)
        reference.child(FirebaseBase.Companion.to64(contact.email!!)).removeValue()
    }
}
