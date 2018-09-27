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
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.navigator.contact

import android.annotation.SuppressLint
import java.util.ArrayList

import io.reactivex.subjects.PublishSubject
import pl.org.seva.navigator.main.instance

val contactsStore get() = instance<Contacts>()

fun addContact(contact: Contact) = contactsStore add contact

fun deleteContact(contact: Contact) = contactsStore delete contact

fun clearAllContacts() = contactsStore.clear()

class Contacts {

    private val contacts: MutableList<Contact>
    private val contactsUpdatedSubject = PublishSubject.create<Contact>()

    init {
        contacts = ArrayList()
    }

    operator fun contains(contact: Contact) = contacts.contains(contact)

    fun snapshot() = ArrayList(contacts)

    infix fun add(contact: Contact) {
        if (contacts.contains(contact)) {
            return
        }
        contacts.add(contact)
        contacts.sort()
        contactsUpdatedSubject.onNext(contact)
    }

    infix fun addAll(contacts: Collection<Contact>) {
        this.contacts.addAll(contacts)
        this.contacts.sort()
    }

    infix fun delete(contact: Contact) {
        contacts.remove(contact)
        contactsUpdatedSubject.onNext(contact)
    }

    fun clear() = contacts.clear()

    operator fun get(position: Int) = contacts[position]

    operator fun get(email: String) = contacts.first { it.email == email }

    fun size() = contacts.size

    @SuppressLint("CheckResult")
    fun addContactsUpdatedListener(email: String?, contactsUpdatedListener : () -> Unit) {
        contactsUpdatedSubject
                .filter { email == null || it.email == email }
                .subscribe { contactsUpdatedListener() }
    }

    fun addContactsUpdatedListener(contactsUpdatedListener: () -> Unit) =
            addContactsUpdatedListener(null, contactsUpdatedListener)
}
