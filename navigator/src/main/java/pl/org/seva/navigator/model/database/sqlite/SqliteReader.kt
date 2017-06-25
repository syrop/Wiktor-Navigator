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

package pl.org.seva.navigator.model.database.sqlite

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Singleton

import pl.org.seva.navigator.model.Contact

@Singleton
class SqliteReader @Inject internal constructor() {

    private var helper: DbHelper? = null

    fun setHelper(helper: DbHelper) {
        this.helper = helper
    }

    val friends: List<Contact>
        get() {
            val result = ArrayList<Contact>()
            val db = helper!!.readableDatabase
            val projection = arrayOf(DbHelper.NAME_COLUMN_NAME, DbHelper.EMAIL_COLUMN_NAME)
            val cursor = db.query(
                    DbHelper.FRIENDS_TABLE_NAME,
                    projection,
                    null, null, null, null, null)

            while (cursor.moveToNext()) {
                val contact = Contact(cursor.getString(0), cursor.getString(1))
                result.add(contact)
            }
            cursor.close()
            db.close()

            return result
        }
}
