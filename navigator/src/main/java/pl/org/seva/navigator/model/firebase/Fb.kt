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

package pl.org.seva.navigator.model.firebase

import android.util.Base64

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import java.util.Locale

import pl.org.seva.navigator.model.Login
import javax.inject.Inject

open class Fb protected constructor() {

    @Inject
    lateinit var login: Login

    protected val db = FirebaseDatabase.getInstance()!!

    protected fun currentUserReference(): DatabaseReference {
        return email2Reference(login.email!!)
    }

    protected fun email2Reference(email: String): DatabaseReference {
        val referencePath = USER_ROOT + "/" + to64(email)
        return db.getReference(referencePath)
    }

    companion object {

        val USER_ROOT = "user"
        val DISPLAY_NAME = "display_name"
        val LAT_LNG = "lat_lng"
        val FRIENDSHIP_REQUESTED = "friendship_requested"
        val FRIENDSHIP_ACCEPTED = "friendship_accepted"
        val FRIENDSHIP_DELETED = "friendship_deleted"
        val FRIENDS = "friends"

        fun to64(str: String) : String = Base64.encodeToString(str.toByteArray(), Base64.NO_WRAP)

        fun from64(str: String) = String(Base64.decode(str.toByteArray(), Base64.NO_WRAP))

        fun latLng2String(latLng: LatLng) = String.format(Locale.US, "%.3f", latLng.latitude) + ";" +
                    String.format(Locale.US, "%.3f", latLng.longitude)

        fun string2LatLng(str: String): LatLng {
            val semicolon = str.indexOf(';')
            val lat = java.lang.Double.parseDouble(str.substring(0, semicolon))
            val lon = java.lang.Double.parseDouble(str.substring(semicolon + 1))
            return LatLng(lat, lon)
        }
    }
}
