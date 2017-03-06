package pl.org.seva.navigator;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import pl.org.seva.navigator.manager.FirebaseDatabaseManager;

import static org.junit.Assert.assertEquals;

public class FirebaseDatabaseManagerTest {

    private static final String LAT = "54.5922815";
    private static final String LON = "-5.9634933";

    @Test
    public void latLng2String() {
        double lat = Double.parseDouble(LAT);
        double lon = Double.parseDouble(LON);
        String str = FirebaseDatabaseManager.latLng2String(new LatLng(lat, lon));
        assertEquals(LAT + ";" + LON, str);
    }

    @Test
    public void string2LatLng() {
        LatLng latLng = FirebaseDatabaseManager.string2LatLng(LAT + ";" + LON);
        assertEquals(LAT, Double.toString(latLng.latitude));
        assertEquals(LON, Double.toString(latLng.longitude));
    }
}
