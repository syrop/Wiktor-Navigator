package pl.org.seva.navigator.model;

import android.support.annotation.NonNull;

public class Contact implements Comparable<Contact> {

    private String email;

    @Override
    public int compareTo(@NonNull Contact o) {
        return email.compareTo(o.email);
    }
}
