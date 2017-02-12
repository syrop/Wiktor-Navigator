package pl.org.seva.navigator.manager;

import java.util.Collections;
import java.util.List;

import pl.org.seva.navigator.model.Contact;

public class ContactsManager {

    private static ContactsManager instance;

    private List<Contact> contacts;

    public static ContactsManager getInstance() {
        if (instance == null) {
            synchronized (ContactsManager.class) {
                if (instance == null) {
                    instance = new ContactsManager();
                }
            }
        }
        return instance;
    }

    private ContactsManager() {
        //
    }

    public void add(Contact contact) {
        contacts.add(contact);
        Collections.sort(contacts);
    }

    public void remove(Contact contact) {
        contacts.remove(contact);
    }
}
