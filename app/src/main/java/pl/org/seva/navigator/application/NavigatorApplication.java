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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import pl.org.seva.navigator.R;
import pl.org.seva.navigator.dagger.DaggerGraph;
import pl.org.seva.navigator.dagger.Graph;
import pl.org.seva.navigator.database.SqliteDataBaseManager;
import pl.org.seva.navigator.model.ActivityRecognitionSource;
import pl.org.seva.navigator.model.ContactsMemoryCache;
import pl.org.seva.navigator.database.FirebaseDatabaseManager;
import pl.org.seva.navigator.model.LocationSource;
import pl.org.seva.navigator.model.Contact;

public class NavigatorApplication extends Application {

    @Inject
    ActivityRecognitionSource activityRecognitionSource;
    @Inject
    SqliteDataBaseManager sqliteDataBaseManager;
    @Inject
    ContactsMemoryCache contactsMemoryCache;
    @Inject FirebaseDatabaseManager firebaseDatabaseManager;
    @Inject LocationSource locationSource;

    private Graph graph;

    private final CompositeDisposable friendshipListeners = new CompositeDisposable();

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
        sqliteDataBaseManager.init(this);
        contactsMemoryCache.addAll(sqliteDataBaseManager.getFriends());
        locationSource.init(this);
        locationSource.locationListener()
                .filter(latLng -> isLoggedIn)
                .subscribe(
                latLng -> firebaseDatabaseManager.onMyLocationReceived(email, latLng)
        );
        if (isLoggedIn) {
            setFriendshipListeners();
        }
    }

    Graph createGraph() {
        return DaggerGraph.create();
    }

    public Graph getGraph() {
        return graph;
    }

    public static Contact getLoggedInContact() {
        return new Contact().setEmail(email).setName(displayName);
    }

    private void setFriendshipListeners() {
        friendshipListeners.addAll(
                firebaseDatabaseManager
                    .friendshipAcceptedListener()
                    .subscribe(NavigatorApplication::onFriendshipAccepted),
                firebaseDatabaseManager
                    .friendshipRequestedListener()
                    .subscribe(this::onFriendshipRequested));
    }

    private void onFriendshipRequested(Contact contact) {
        String message = getResources()
                .getString(R.string.friendship_confirmation)
                .replace("[name]", contact.name())
                .replace("[email]", contact.email());
        Intent friendshipAcceptedIntent = new Intent().putExtra(Contact.PARCELABLE_NAME, contact);
        PendingIntent noPi = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, friendshipAcceptedIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // http://stackoverflow.com/questions/6357450/android-multiline-notifications-notifications-with-longer-text#22964072
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(getString(R.string.app_name));
        bigTextStyle.bigText(message);

        // http://stackoverflow.com/questions/11883534/how-to-dismiss-notification-after-action-has-been-clicked#11884313
        Notification notification = new NotificationCompat.Builder(this)
                .setStyle(bigTextStyle)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_close_black_24dp, getString(android.R.string.no), noPi)
                .addAction(R.drawable.ic_check_black_24dp, getString(android.R.string.yes), pi)
                .build();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, notification);
    }

    private static void onFriendshipAccepted(Contact contact) {

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
