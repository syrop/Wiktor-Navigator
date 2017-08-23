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

package pl.org.seva.navigator.data

import android.annotation.SuppressLint
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import pl.org.seva.navigator.data.room.ContactsDatabase

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = ContactsDatabase.TABLE_NAME)
data class Contact(
        @PrimaryKey var email: String = "", var name: String = "") : Comparable<Contact>, Parcelable {

    @Ignore
    @Transient
    val isEmpty = email.isEmpty()

    override fun compareTo(other: Contact): Int {
        var result = name.compareTo(other.name)
        if (result == 0) {
            result = email.compareTo(other.email)
        }
        return result
    }

    override fun equals(other: Any?) =
        !(other == null || other !is Contact) && email == other.email

    override fun hashCode() = email.hashCode()
}
