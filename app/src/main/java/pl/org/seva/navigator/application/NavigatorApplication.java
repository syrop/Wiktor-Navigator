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

package pl.org.seva.navigator.application;

import android.app.Application;
import android.support.v7.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.disposables.CompositeDisposable;
import pl.org.seva.navigator.R;
import pl.org.seva.navigator.manager.ActivityRecognitionManager;
import pl.org.seva.navigator.manager.ContactManager;
import pl.org.seva.navigator.manager.DatabaseManager;
import pl.org.seva.navigator.manager.FirebaseDatabaseManager;
import pl.org.seva.navigator.manager.GpsManager;
import pl.org.seva.navigator.model.Contact;

public class NavigatorApplication extends Application {

    private final CompositeDisposable friendshipListeners = new CompositeDisposable();

    public static boolean isLoggedIn;
    public static String email;
    public static String displayName;

    @Override
    public void onCreate() {
        super.onCreate();
        setCurrentFirebaseUser(FirebaseAuth.getInstance().getCurrentUser());
        ActivityRecognitionManager.getInstance().init(this);
        DatabaseManager.getInstance().init(this);
        ContactManager.getInstance().addAll(DatabaseManager.getInstance().getFriends());
        GpsManager.getInstance().init(this);
        GpsManager.getInstance().locationListener()
                .filter(latLng -> isLoggedIn)
                .subscribe(
                latLng -> FirebaseDatabaseManager.getInstance().onLocationReceived(email, latLng)
        );
        if (isLoggedIn) {
            setFriendshipListeners();
        }
    }

    public static Contact getLoggedInContact() {
        return new Contact().setEmail(email).setName(displayName);
    }

    private void setFriendshipListeners() {
        friendshipListeners.addAll(
                FirebaseDatabaseManager
                    .getInstance()
                    .friendshipAcceptedListener()
                    .subscribe(NavigatorApplication::onFriendshipAccepted),
                FirebaseDatabaseManager
                    .getInstance()
                    .friendshipRequestedListener()
                    .subscribe(this::onFriendshipRequested));
    }

    private void onFriendshipRequested(Contact contact) {
        String message = getResources()
                .getString(R.string.friendship_confirmation)
                .replace("[name]", contact.name())
                .replace("[email]", contact.email());
        new AlertDialog
                .Builder(this)
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, ((dialog, which) -> onFriendshipAccepted(contact)))
                .setNegativeButton(android.R.string.no, ((dialog, which) -> {}))
                .create()
                .show();
    }

    private static void onFriendshipAccepted(Contact contact) {
        ContactManager.getInstance().add(contact);
        DatabaseManager.getInstance().persistFriend(contact);
    }

    public void login(FirebaseUser user) {
        setCurrentFirebaseUser(user);
        setFriendshipListeners();
    }

    public void logout() {
        friendshipListeners.clear();
        setCurrentFirebaseUser(null);
    }

    private static void setCurrentFirebaseUser(FirebaseUser user) {
        if (user != null) {
            isLoggedIn = true;
            email = user.getEmail();
            displayName = user.getDisplayName();
        }
        else {
            isLoggedIn = false;
            email = null;
            displayName = null;
        }
    }
}
