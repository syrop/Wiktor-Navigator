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

package pl.org.seva.navigator.model.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) :
        SQLiteOpenHelper(context, DbHelper.DATABASE_NAME, null, DbHelper.DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) = db.execSQL(CREATION_STATEMENT)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {

        val FRIENDS_TABLE_NAME = "friends"
        val EMAIL_COLUMN_NAME = "email"
        val NAME_COLUMN_NAME = "name"

        val DATABASE_VERSION = 1
        val DATABASE_NAME = "Friends.db"

        val CREATION_STATEMENT =
                "create table if not exists $FRIENDS_TABLE_NAME (id integer primary key autoincrement, " +
                        "$EMAIL_COLUMN_NAME text, " +
                        "$NAME_COLUMN_NAME text, " +
                        "constraint unique_name unique ($EMAIL_COLUMN_NAME))"
    }
}
