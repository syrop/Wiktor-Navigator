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

package pl.org.seva.navigator;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import pl.org.seva.navigator.database.FirebaseDatabaseWriter;
import pl.org.seva.navigator.database.FirebaseUtils;

import static org.junit.Assert.assertEquals;

public class FirebaseDatabaseWriterTest {

    private static final String LAT = "54.5922815";
    private static final String LON = "-5.9634933";

    @Test
    public void latLng2String() {
        double lat = Double.parseDouble(LAT);
        double lon = Double.parseDouble(LON);
        String str = FirebaseUtils.latLng2String(new LatLng(lat, lon));
        assertEquals(LAT + ";" + LON, str);
    }

    @Test
    public void string2LatLng() {
        LatLng latLng = FirebaseUtils.string2LatLng(LAT + ";" + LON);
        assertEquals(LAT, Double.toString(latLng.latitude));
        assertEquals(LON, Double.toString(latLng.longitude));
    }
}
