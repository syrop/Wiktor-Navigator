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

package pl.org.seva.navigator.model

import android.os.Parcel
import android.os.Parcelable

class Contact() : Comparable<Contact>, Parcelable {

    var email: String? = null
    var name: String? = null

    val isEmpty: Boolean
        get() = email == null || name == null

    constructor(email: String, name: String): this() {
        this.email = email
        this.name = name
    }

    override fun compareTo(other: Contact): Int {
        var result = name!!.compareTo(other.name!!)
        if (result == 0) {
            result = email!!.compareTo(other.email!!)
        }
        return result
    }

    override fun equals(other: Any?) =
        !(other == null || other !is Contact) && email == other.email

    override fun hashCode() = email!!.hashCode()

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(email)
        dest.writeString(name)
    }

    private constructor(parcel: Parcel): this(parcel.readString(), parcel.readString())

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR = object : Parcelable.Creator<Contact> {
            override fun createFromParcel(parcel: Parcel) = Contact(parcel)
            override fun newArray(size: Int): Array<Contact?> = arrayOfNulls(size)
        }
    }
}
