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
 * If you like this program, consider donating bitcoin: 3JVNWUeVH118S3pzU4hDgkUNwEeNarZySf
 */

package pl.org.seva.navigator.contact.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.main.instance

infix fun ContactDao.insert(contact: Contact) = insert(contact.toEntity())

infix fun ContactDao.delete(contact: Contact) = delete(contact.toEntity())

fun contactDao() = instance<ContactDao>()

@Dao
interface ContactDao {

    @Query("SELECT * FROM ${ContactsDatabase.TABLE_NAME}")
    fun getAll(): List<ContactEntity>

    @Insert
    fun insert(contact: ContactEntity)

    @Delete
    fun delete(contact: ContactEntity)

    @Query("DELETE FROM ${ContactsDatabase.TABLE_NAME}")
    fun deleteAll(): Int
}
