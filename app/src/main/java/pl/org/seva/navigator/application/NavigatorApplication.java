package pl.org.seva.navigator.application;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import pl.org.seva.navigator.manager.ActivityRecognitionManager;
import pl.org.seva.navigator.manager.GpsManager;

public class NavigatorApplication extends Application {

    public static boolean isLoggedIn;
    public static String uid;
    public static String email;
    public static String displayName;

    @Override
    public void onCreate() {
        super.onCreate();
        setCurrentFirebaseUser(FirebaseAuth.getInstance().getCurrentUser());
        ActivityRecognitionManager.getInstance().init(this);
        GpsManager.getInstance().init(this);
    }

    public static void setCurrentFirebaseUser(FirebaseUser user) {
        if (user != null) {
            isLoggedIn = true;
            uid = user.getUid();
            email = user.getEmail();
            displayName = user.getDisplayName();
        }
        else {
            isLoggedIn = false;
            uid = null;
            email = null;
            displayName = null;
        }
    }
}
