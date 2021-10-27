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
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.navigator.contact

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.core.content.edit
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import pl.org.seva.navigator.contact.Contact.Companion.CONTACT_EMAIL_PROPERTY
import pl.org.seva.navigator.contact.Contact.Companion.CONTACT_NAME_PROPERTY
import pl.org.seva.navigator.main.data.db.ContactsDatabase

infix fun Contact?.persist(prefs: SharedPreferences) {
    val name = this?.name ?: ""
    val email = this?.email ?: ""
    prefs.edit {
        putString(CONTACT_NAME_PROPERTY, name)
        putString(CONTACT_EMAIL_PROPERTY, email)
    }
}

fun readContactFromProperties(prefs: SharedPreferences): Contact? {
    val name = checkNotNull(prefs.getString(CONTACT_NAME_PROPERTY, ""))
    val email = checkNotNull(prefs.getString(CONTACT_EMAIL_PROPERTY, ""))
    if (name.isNotEmpty() && email.isNotEmpty()) {
        val contact = Contact(email = email, name = name)
        if (contact in contacts) {
            return contact
        }
    }
    return null
}

@SuppressLint("ParcelCreator")
@Parcelize
data class Contact(
        val email: String = "",
        val name: String = "",
        val color: Int = Color.GRAY,
        val debugVersion: Int = 0) : Comparable<Contact>, Parcelable {

    @IgnoredOnParcel
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

    fun toEntity() = Entity(this)

    @androidx.room.Entity(tableName = ContactsDatabase.TABLE_NAME)
    class Entity() {
        @PrimaryKey
        @NonNull
        lateinit var email: String
        lateinit var name: String
        var color = Color.GRAY
        var debugVersion = 0

        constructor(contact: Contact) : this() {
            email = contact.email
            name = contact.name
            color = contact.color
        }

        fun value() = Contact(email, name, color)
    }

    companion object {

        const val CONTACT_NAME_PROPERTY = "navigation_map_followed_name"
        const val CONTACT_EMAIL_PROPERTY = "navigation_map_followed_email"
    }
}
