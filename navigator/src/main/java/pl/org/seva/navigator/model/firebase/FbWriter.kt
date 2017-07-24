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

package pl.org.seva.navigator.model.firebase

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference

import javax.inject.Inject
import javax.inject.Singleton

import pl.org.seva.navigator.model.Contact

@Singleton
class FbWriter @Inject
internal constructor() : Fb() {

    fun login(user: FirebaseUser) {
        val contact = Contact(user.email!!, user.displayName!!)
        writeContact(db.getReference(USER_ROOT), contact)
    }

    fun writeMyLocation(email: String, latLng: LatLng) {
        email2Reference(email).child(LAT_LNG).setValue(latLng2String(latLng))
    }

    fun requestFriendship(contact: Contact) {
        val reference = email2Reference(contact.email!!).child(FRIENDSHIP_REQUESTED)
        writeContact(reference, login.loggedInContact)
    }

    fun acceptFriendship(contact: Contact) {
        val reference = email2Reference(contact.email!!).child(FRIENDSHIP_ACCEPTED)
        writeContact(reference, login.loggedInContact)
        addFriendship(contact)
    }

    fun addFriendship(contact: Contact) {
        val reference = email2Reference(login.email!!).child(FRIENDS)
        deleteMeFromContact(contact, FRIENDSHIP_DELETED)
        writeContact(reference, contact)
    }

    fun deleteFriendship(contact: Contact) {
        val reference = email2Reference(contact.email!!).child(FRIENDSHIP_DELETED)
        writeContact(reference, login.loggedInContact)
        deleteContactFromMe(contact, FRIENDS)
    }

    private fun writeContact(reference: DatabaseReference, contact: Contact) {
        val contactEmail = to64(contact.email!!)
        val localReference = reference.child(contactEmail)
        localReference.child(DISPLAY_NAME).setValue(contact.name!!)
    }

    private fun deleteMeFromContact(contact: Contact, tag: String) {
        val reference = email2Reference(contact.email!!).child(tag)
        reference.child(to64(login.email!!)).removeValue()
    }

    private fun deleteContactFromMe(contact: Contact, tag: String) {
        val reference = email2Reference(login.email!!).child(tag)
        reference.child(to64(contact.email!!)).removeValue()
    }

    fun deleteMe() {
        val reference = email2Reference(login.email!!)
        reference.removeValue()
    }
}
