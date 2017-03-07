package pl.org.seva.navigator.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.model.FriendsDbHelper;

public class DatabaseManager {

    public static final String FRIENDS_TABLE_NAME = "friends";
    public static final String EMAIL_COLUMN_NAME = "email";
    public static final String NAME_COLUMN_NAME = "name";

    private FriendsDbHelper helper;

    private static DatabaseManager instance;

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private DatabaseManager() {
        //
    }

    public void init(Context context) {
        if (helper != null) {
            throw new IllegalStateException("Database already initialized");
        }
        helper = new FriendsDbHelper(context);
    }

    public void addFriend(Contact contact) {
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
