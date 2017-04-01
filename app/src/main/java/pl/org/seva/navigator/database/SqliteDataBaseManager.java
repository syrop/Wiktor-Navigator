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

package pl.org.seva.navigator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.navigator.model.Contact;

@Singleton
public class SqliteDataBaseManager {

    static final String FRIENDS_TABLE_NAME = "friends";
    static final String EMAIL_COLUMN_NAME = "email";
    static final String NAME_COLUMN_NAME = "name";

    private SqliteDbHelper helper;

    @Inject SqliteDataBaseManager() {
    }

    public void init(Context context) {
        if (helper != null) {
            throw new IllegalStateException("Database already initialized");
        }
        helper = new SqliteDbHelper(context);
    }

    public void persistFriend(Contact contact) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(NAME_COLUMN_NAME, contact.name());
        cv.put(EMAIL_COLUMN_NAME, contact.email());
        db.insert(FRIENDS_TABLE_NAME, null, cv);
        db.close();
    }

    public void deleteFriend(Contact contact) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String query = EMAIL_COLUMN_NAME + " equals ?";
        String[] args = { contact.email(), };
        db.delete(FRIENDS_TABLE_NAME, query, args);
        db.close();
    }

    public List<Contact> getFriends() {
        List<Contact> result = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String[] projection = { NAME_COLUMN_NAME, EMAIL_COLUMN_NAME, };
        Cursor cursor = db.query(
                FRIENDS_TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setName(cursor.getString(0));
            contact.setEmail(cursor.getString(1));
            result.add(contact);
        }
        cursor.close();
        db.close();

        return result;
    }
}
