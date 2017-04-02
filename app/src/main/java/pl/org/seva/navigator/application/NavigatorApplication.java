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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import pl.org.seva.navigator.dagger.DaggerGraph;
import pl.org.seva.navigator.dagger.Graph;
import pl.org.seva.navigator.database.sqlite.DbHelper;
import pl.org.seva.navigator.database.sqlite.SqliteReader;
import pl.org.seva.navigator.database.sqlite.SqliteWriter;
import pl.org.seva.navigator.receiver.FriendshipReceiver;
import pl.org.seva.navigator.receiver.MyLocationReceiver;
import pl.org.seva.navigator.source.ActivityRecognitionSource;
import pl.org.seva.navigator.model.ContactsMemoryCache;
import pl.org.seva.navigator.source.FriendshipSource;
import pl.org.seva.navigator.source.MyLocationSource;
import pl.org.seva.navigator.model.Contact;

public class NavigatorApplication extends Application {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject ActivityRecognitionSource activityRecognitionSource;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject SqliteWriter sqliteWriter;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject SqliteReader sqliteReader;
    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject ContactsMemoryCache contactsMemoryCache;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject MyLocationSource myLocationSource;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject MyLocationReceiver myLocationReceiver;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject FriendshipSource friendshipSource;
    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject FriendshipReceiver friendshipReceiver;

    private Graph graph;

    public static boolean isLoggedIn;
    public static String email;
    public static String displayName;

    @Override
    public void onCreate() {
        super.onCreate();
        graph = createGraph();
        graph.inject(this);
        setCurrentFirebaseUser(FirebaseAuth.getInstance().getCurrentUser());
        activityRecognitionSource.init(this);
        DbHelper helper = new DbHelper(this);
        sqliteWriter.setHelper(helper);
        sqliteReader.setHelper(helper);
        contactsMemoryCache.addAll(sqliteReader.getFriends());
        myLocationSource.init(this).addLocationReceiver(myLocationReceiver);
        friendshipReceiver.init(this);
        if (isLoggedIn) {
            setFriendshipListeners();
        }
    }

    private Graph createGraph() {
        return DaggerGraph.create();
    }

    public Graph getGraph() {
        return graph;
    }

    public static Contact getLoggedInContact() {
        return new Contact().setEmail(email).setName(displayName);
    }

    public void login(FirebaseUser user) {
        setCurrentFirebaseUser(user);
        setFriendshipListeners();
    }

    public void logout() {
        clearFriendshipListeners();
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

    private void setFriendshipListeners() {
        friendshipSource.addFriendshipReceiver(friendshipReceiver);
    }

    private void clearFriendshipListeners() {
        friendshipSource.clearFriendshipReceivers();
    }
}
