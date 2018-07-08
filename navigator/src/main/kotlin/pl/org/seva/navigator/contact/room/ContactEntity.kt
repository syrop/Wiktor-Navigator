/*
 * Copyright (C) 2018 Wiktor Nizio
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
 * If you like this program, consider donating bitcoin: 3JVNWUeVH118S3pzU4hDgkUNwEeNarZySf
 */

package pl.org.seva.navigator.contact.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.graphics.Color
import pl.org.seva.navigator.contact.Contact

@Entity(tableName = ContactsDatabase.TABLE_NAME)
class ContactEntity() {
    @PrimaryKey
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
