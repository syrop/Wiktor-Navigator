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

package pl.org.seva.navigator.main.db

import androidx.room.Room
import pl.org.seva.navigator.main.appContext
import pl.org.seva.navigator.main.db.migration.AddedColorMigration
import pl.org.seva.navigator.main.db.migration.AddedDebugMigration
import pl.org.seva.navigator.main.db.migration.LiteToRoomMigration
import pl.org.seva.navigator.main.instance

val contactsDatabase by instance<ContactsDatabase>()

class ContactsDatabase {

    private val db: NavigatorDbAbstract

    init {
        db = Room.databaseBuilder(appContext, NavigatorDbAbstract::class.java, DATABASE_NAME)
                .addMigrations(LiteToRoomMigration(), AddedColorMigration(), AddedDebugMigration())
                .allowMainThreadQueries()
                .build()
    }

    val contactDao get() = db.contactDao()

    companion object {
        const val SQL_LITE_DATABASE_VERSION = 1
        const val ROOM_DATABASE_VERSION = 2
        const val ADDED_COLOR_DATABASE_VERSION = 3
        const val ADDED_DEBUG_DATABASE_VERSION = 5
        const val DATABASE_NAME = "Friends.db"
        const val TABLE_NAME = "friends"
    }
}
