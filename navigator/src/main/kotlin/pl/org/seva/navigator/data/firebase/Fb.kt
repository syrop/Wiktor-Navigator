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

package pl.org.seva.navigator.data.firebase

import android.util.Base64

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import pl.org.seva.navigator.main.instance

import java.util.Locale

import pl.org.seva.navigator.profile.LoggedInUser

open class Fb {

    protected val loggedInUser: LoggedInUser = instance()

    protected val db = FirebaseDatabase.getInstance()!!

    protected fun currentUserReference() = loggedInUser.email!!.toReference()

    protected fun String.toReference() = db.getReference(USER_ROOT + "/" + to64())!!

    fun String.to64() = Base64.encodeToString(toByteArray(), Base64.NO_WRAP)!!

    fun String.from64() = String(Base64.decode(toByteArray(), Base64.NO_WRAP))

    companion object {
        val USER_ROOT = "user"
        val DISPLAY_NAME = "display_name"
        val LAT_LNG = "lat_lng"
        val FRIENDSHIP_REQUESTED = "friendship_requested"
        val FRIENDSHIP_ACCEPTED = "friendship_accepted"
        val FRIENDSHIP_DELETED = "friendship_deleted"
        val FRIENDS = "friends"

        fun LatLng.toFbString() = String.format(Locale.US, "%.3f", latitude) + ";" +
                String.format(Locale.US, "%.3f", longitude)

        fun String.toLatLng(): LatLng {
            val semicolon = indexOf(';')
            val lat = java.lang.Double.parseDouble(substring(0, semicolon))
            val lon = java.lang.Double.parseDouble(substring(semicolon + 1))
            return LatLng(lat, lon)
        }
    }
}
