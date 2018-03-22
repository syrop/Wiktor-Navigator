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

package pl.org.seva.navigator

import com.google.android.gms.maps.model.LatLng
import pl.org.seva.navigator.data.firebase.Fb.Companion.toFbString
import pl.org.seva.navigator.data.firebase.Fb.Companion.toLatLng

import org.junit.Test

import org.junit.Assert.assertEquals

class FirebaseTest {

    @Test
    fun latLng2String() {
        val lat = java.lang.Double.parseDouble(LAT)
        val lon = java.lang.Double.parseDouble(LON)
        val str = (LatLng(lat, lon)).toFbString()
        assertEquals(LAT + ";" + LON, str)
    }

    @Test
    fun string2LatLng() {
        val latLng = "$LAT+$LON".toLatLng()
        assertEquals(LAT, java.lang.Double.toString(latLng.latitude))
        assertEquals(LON, java.lang.Double.toString(latLng.longitude))
    }

    companion object {

        private val LAT = "54.5922815"
        private val LON = "-5.9634933"
    }
}
