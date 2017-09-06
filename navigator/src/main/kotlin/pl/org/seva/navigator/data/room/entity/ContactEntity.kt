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

package pl.org.seva.navigator.data.room.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import pl.org.seva.navigator.data.model.Contact
import pl.org.seva.navigator.data.room.ContactsDatabase

@Entity(tableName = ContactsDatabase.TABLE_NAME)
class ContactEntity() {
    @PrimaryKey
    lateinit var email: String
    lateinit var name: String
    var color: Int = 0

    constructor(contact: Contact): this() {
        email = contact.email
        name = contact.name
        color = contact.color
    }

    fun contactValue() = Contact(email, name, color)
}
