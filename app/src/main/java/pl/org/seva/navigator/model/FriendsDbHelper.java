package pl.org.seva.navigator.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import pl.org.seva.navigator.manager.DatabaseManager;

public class FriendsDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Friends.db";

    private static final String CREATION_STATEMENT =
            "create table if not exists " + DatabaseManager.FRIENDS_TABLE_NAME +
                    " (id integer primary key autoincrement, " +
                    DatabaseManager.EMAIL_COLUMN_NAME + " text, " +
                    DatabaseManager.NAME_COLUMN_NAME + " text)";

    public FriendsDbHelper(Context context) {
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
