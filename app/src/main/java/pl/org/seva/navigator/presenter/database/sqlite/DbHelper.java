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

package pl.org.seva.navigator.presenter.database.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    static final String FRIENDS_TABLE_NAME = "friends";
    static final String EMAIL_COLUMN_NAME = "email";
    static final String NAME_COLUMN_NAME = "name";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Friends.db";

    private static final String CREATION_STATEMENT =
            "create table if not exists " + FRIENDS_TABLE_NAME +
                    " (id integer primary key autoincrement, " +
                    EMAIL_COLUMN_NAME + " text, " +
                    NAME_COLUMN_NAME + " text)";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATION_STATEMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
