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

package pl.org.seva.navigator.model.room

import android.arch.persistence.room.Room
import android.content.Context

class ContactsDatabase {

    lateinit var db: ContactsDatabaseAbstract

    fun initWithContext(context: Context) {
        db = Room.databaseBuilder(context, ContactsDatabaseAbstract::class.java, DATABASE_NAME)
                .addMigrations(LiteToRoomMigration())
                .allowMainThreadQueries()
                .build()
    }

    val contactDao get() = db.contactDao()

    companion object {
        const val SQL_DATABASE_VERSION = 1
        const val ROOM_DATABASE_VERSION = 2
        val DATABASE_NAME = "Friends.db"
        const val TABLE_NAME = "friends"
    }
}
