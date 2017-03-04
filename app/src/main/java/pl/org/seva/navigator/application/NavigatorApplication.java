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

import pl.org.seva.navigator.manager.ActivityRecognitionManager;
import pl.org.seva.navigator.manager.ContactManager;
import pl.org.seva.navigator.manager.DatabaseManager;
import pl.org.seva.navigator.manager.GpsManager;

public class NavigatorApplication extends Application {

    public static boolean isLoggedIn;
    public static String email;
    public static String displayName;

    @Override
    public void onCreate() {
        super.onCreate();
        setCurrentFirebaseUser(FirebaseAuth.getInstance().getCurrentUser());
        ActivityRecognitionManager.getInstance().init(this);
        GpsManager.getInstance().init(this);
        GpsManager.getInstance().locationListener()
                .filter(latLng -> isLoggedIn)
                .subscribe(
                latLng -> DatabaseManager.getInstance().onLocationReceived(email, latLng)
        );
        DatabaseManager
                .getInstance()
                .friendshipAcceptedListener()
                .subscribe(contact -> ContactManager.getInstance().onFriendshipAccepted(contact));
    }

    public static void setCurrentFirebaseUser(FirebaseUser user) {
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
