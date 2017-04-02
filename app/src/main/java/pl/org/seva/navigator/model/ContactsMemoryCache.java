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

package pl.org.seva.navigator.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.navigator.NavigatorApplication;

@Singleton
public class ContactsMemoryCache {

    private final List<Contact> contacts;

    @Inject public ContactsMemoryCache() {
        contacts = new ArrayList<>();
    }

    private Contact getMe() {
        return new Contact()
                .setEmail(NavigatorApplication.email)
                .setName(NavigatorApplication.displayName);
    }

    public boolean contains(Contact contact) {
        return getMe().equals(contact) || contacts.contains(contact);
    }

    public void add(Contact contact) {
        contacts.add(contact);
        Collections.sort(contacts);
    }

    public void addAll(Collection<Contact> contacts) {
        this.contacts.addAll(contacts);
        Collections.sort(this.contacts);
    }

    public void delete(Contact contact) {
        contacts.remove(contact);
    }

    public Contact get(int position) {
        return position == 0 ? getMe() : contacts.get(position - 1);
    }

    public int size() {
        return contacts.size() + 1;
    }
}
