package pl.org.seva.navigator.model;

import android.support.annotation.NonNull;

public class Contact implements Comparable<Contact> {

    public String email;
    public String displayName;
    private String uid;

    public Contact setName(String name) {
        this.displayName = name;
        return this;
    }

    public Contact setEmail(String email) {
        this.email = email;
        return this;
    }

    public Contact setUid(String uid) {
        this.uid = uid;
        return this;
    }

    @Override
    public int compareTo(@NonNull Contact o) {
        return displayName.compareTo(o.displayName);
    }
}
