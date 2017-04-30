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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.navigator.NavigatorApplication;
import pl.org.seva.navigator.model.Contact;

@Singleton
public class FirebaseWriter extends FirebaseBase {

    @SuppressWarnings("WeakerAccess")
    @Inject
    FirebaseWriter() {
        super();
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

    public void storeMyLocation(String email, LatLng latLng) {
        email2Reference(email).child(LAT_LNG).setValue(latLng2String(latLng));
    }

    public void requestFriendship(Contact contact) {
        DatabaseReference reference = email2Reference(contact.email()).child(FRIENDSHIP_REQUESTS);
        writeContact(reference, NavigatorApplication.getLoggedInContact());
    }

    public void acceptFriendship(Contact contact) {
        DatabaseReference reference = email2Reference(contact.email()).child(FRIENDSHIP_ACCEPTED);
        writeContact(reference, NavigatorApplication.getLoggedInContact());
    }

    public void deleteFriendship(Contact contact) {
        DatabaseReference reference = email2Reference(contact.email()).child(FRIENDSHIP_ACCEPTED);
        writeContact(reference, NavigatorApplication.getLoggedInContact());
    }
}
