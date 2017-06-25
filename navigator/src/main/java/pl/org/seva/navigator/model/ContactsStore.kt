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

package pl.org.seva.navigator.model

import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.subjects.PublishSubject
import pl.org.seva.navigator.NavigatorApplication

@Singleton
class ContactsStore @Inject constructor() {

    private val contacts: MutableList<Contact>
    private val contactsUpdatedSubject = PublishSubject.create<Contact>()

    init {
        contacts = ArrayList<Contact>()
    }

    private val me: Contact
        get() = Contact(NavigatorApplication.email!!, NavigatorApplication.displayName!!)

    operator fun contains(contact: Contact): Boolean {
        return me == contact || contacts.contains(contact)
    }

    fun add(contact: Contact) {
        if (contacts.contains(contact)) {
            return
        }
        contacts.add(contact)
        Collections.sort(contacts)
        contactsUpdatedSubject.onNext(contact)
    }

    fun addAll(contacts: Collection<Contact>) {
        this.contacts.addAll(contacts)
        Collections.sort(this.contacts)
    }

    fun delete(contact: Contact) {
        contacts.remove(contact)
        contactsUpdatedSubject.onNext(contact)
    }

    fun clear() = contacts.clear()

    operator fun get(position: Int): Contact {
        return if (position == 0) me else contacts[position - 1]
    }

    fun size(): Int {
        return contacts.size + 1
    }

    fun addContactsUpdatedListener(email: String?, contactsUpdatedListener : () -> Unit) {
        contactsUpdatedSubject
                .filter { email == null || it.email == email }
                .subscribe { contactsUpdatedListener() }
    }

    fun addContactsUpdatedListener(contactsUpdatedListener: () -> Unit) {
        addContactsUpdatedListener(null, contactsUpdatedListener)
    }
}
