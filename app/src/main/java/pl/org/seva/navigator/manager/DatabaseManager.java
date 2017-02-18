package pl.org.seva.navigator.manager;

import android.util.Base64;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DatabaseManager {

    private static final String USER = "user";
    private static final String UID = "uid";
    private static final String DISPLAY_NAME = "display_name";
    private static final String LAT = "lat";
    private static final String LON = "lon";

    private static String to64(String str) {
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

    private static DatabaseManager instance;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private DatabaseManager() {
        //
    }

    public void login(FirebaseUser user) {
        String email64 = to64(user.getEmail());
        DatabaseReference userReference = database.getReference(USER);
        userReference.setValue(email64);
        userReference = userReference.child(email64);
        userReference.child(UID).setValue(user.getUid());
        userReference.child(DISPLAY_NAME).setValue(to64(user.getDisplayName()));
    }

    public void location(String email, LatLng latLng) {
        String email64 = to64(email);
        DatabaseReference ref = database.getReference(USER).child(email64);
        ref.child(LAT).setValue(latLng.latitude);
        ref.child(LON).setValue(latLng.longitude);
    }
}
