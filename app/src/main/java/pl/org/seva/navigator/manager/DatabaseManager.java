package pl.org.seva.navigator.manager;

import pl.org.seva.navigator.model.Contact;

public class DatabaseManager {

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

    public void createFriendsTable() {

    }

    public boolean doesFriendsTableExist() {
        return false;
    }

    public void addFriend(Contact contact) {

    }

    public void deleteFriend(Contact contact) {

    }
}
