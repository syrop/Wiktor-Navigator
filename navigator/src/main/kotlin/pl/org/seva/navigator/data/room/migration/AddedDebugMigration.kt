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
 * If you like this program, consider donating bitcoin: 36uxha7sy4mv6c9LdePKjGNmQe8eK16aX6
 */

package pl.org.seva.navigator.data.room.migration

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import pl.org.seva.navigator.contact.room.ContactsDatabase

class AddedDebugMigration : Migration(
        ContactsDatabase.ADDED_COLOR_DATABASE_VERSION,
        ContactsDatabase.ADDED_DEBUG_DATABASE_VERSION) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(RENAME_STATEMENT)
        database.execSQL(CREATION_STATEMENT)
        val cursor = database.query(SELECT_STATEMENT)
        cursor.use {
            while (it.moveToNext()) {
                val cv = ContentValues()
                cv.put("email", cursor.getString(0))
                cv.put("name", cursor.getString(1))
                cv.put("color", cursor.getInt(2))
                cv.put("debugVersion", 0)
                database.insert("friends", SQLiteDatabase.CONFLICT_NONE, cv)
            }
        }
        database.execSQL(DROP_STATEMENT)
    }

    companion object {
        private const val RENAME_STATEMENT =
                "alter table friends rename to friends_old"
        private const val CREATION_STATEMENT =
                "create table if not exists friends (email TEXT primary key not null, name TEXT not null, color INTEGER not null, debugVersion INTEGER not null)"
        private const val SELECT_STATEMENT =
                "select * from friends_old"
        private const val DROP_STATEMENT =
                "drop table friends_old"
    }

}