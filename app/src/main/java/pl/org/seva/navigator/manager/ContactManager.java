package pl.org.seva.navigator.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.model.Contact;

public class ContactManager {

    private static ContactManager instance;

    private List<Contact> contacts;

    public static ContactManager getInstance() {
        if (instance == null) {
            synchronized (ContactManager.class) {
                if (instance == null) {
                    instance = new ContactManager();
                }
            }
        }
        return instance;
    }

    private ContactManager() {
        contacts = new ArrayList<>();
    }

    private Contact getMe() {
        return new Contact()
                .setEmail(NavigatorApplication.email)
                .setName(NavigatorApplication.displayName)
                .setUid(NavigatorApplication.uid);
    }

    public void add(Contact contact) {
        contacts.add(contact);
        Collections.sort(contacts);
    }

    public void remove(Contact contact) {
        contacts.remove(contact);
    }

    public Contact get(int position) {
        return position == 0 ? getMe() : contacts.get(position + 1);
    }

    public int size() {
        return contacts.size() + 1;
    }
}
