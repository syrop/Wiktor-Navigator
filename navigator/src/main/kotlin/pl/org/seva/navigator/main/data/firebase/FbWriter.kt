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

package pl.org.seva.navigator.main.data.firebase

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference

import pl.org.seva.navigator.contacts.Contact

class FbWriter : Fb() {

    infix fun login(user: FirebaseUser) {
        val contact = Contact(user.email!!, user.displayName!!)
        contact.write(db.getReference(USER_ROOT))
    }

    infix fun writeMyLocation(latLng: LatLng) {
        loggedInUser.email!!.toReference().child(LAT_LNG).setValue(latLng.toFbString())
    }

    infix fun requestFriendship(contact: Contact) =
            contact.email.toReference().child(FRIENDSHIP_REQUESTED).write(loggedInUser.loggedInContact)

    infix fun acceptFriendship(contact: Contact) {
        contact.email.toReference().child(FRIENDSHIP_ACCEPTED).write(loggedInUser.loggedInContact)
        addFriendship(contact)
    }

    infix fun addFriendship(contact: Contact) {
        loggedInUser.email!!.toReference().child(FRIENDS).write(contact)
        contact.deleteMeFromTag(FRIENDSHIP_DELETED)
    }

    infix fun deleteFriendship(contact: Contact) {
        contact.email.toReference().child(FRIENDSHIP_DELETED).write(loggedInUser.loggedInContact)
        contact.deleteFromMyFriends()
    }

    fun deleteMe() {
        loggedInUser.email!!.toReference().removeValue()
    }

    private fun Contact.write(reference: DatabaseReference) = reference.write(this)

    private fun DatabaseReference.write(contact: Contact) {
        val contactEmail = contact.email.to64()
        child(contactEmail).child(DISPLAY_NAME).setValue(contact.name)
    }

    private fun Contact.deleteMeFromTag(tag: String) =
        email.toReference().child(tag).child(loggedInUser.email!!.to64()).removeValue()

    private fun Contact.deleteFromMyFriends() =
        loggedInUser.email!!.toReference().child(FRIENDS).child(email.to64()).removeValue()
}
