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
import io.reactivex.disposables.Disposable
import java.util.ArrayList

import io.reactivex.subjects.PublishSubject
import pl.org.seva.navigator.main.fb.fbReader
import pl.org.seva.navigator.main.instance

val contacts by instance<Contacts>()

fun downloadFriendsFromFb(onFriendFound: (Contact) -> Unit, onCompleted: () -> Unit): Disposable =
        fbReader.readFriends().doOnComplete(onCompleted).subscribe { onFriendFound(it) }

class Contacts {

    private val contactsCache = ArrayList<Contact>()
    private val contactsUpdatedSubject = PublishSubject.create<Contact>()

    operator fun contains(contact: Contact) = contactsCache.contains(contact)

    fun snapshot() = ArrayList(contactsCache)

    infix fun add(contact: Contact) {
        if (contactsCache.contains(contact)) {
            return
        }
        contactsCache.add(contact)
        contactsCache.sort()
        contactsUpdatedSubject.onNext(contact)
    }

    infix fun addAll(contacts: Collection<Contact>) {
        this.contactsCache.addAll(contacts)
        this.contactsCache.sort()
    }

    infix fun delete(contact: Contact) {
        contactsCache.remove(contact)
        contactsUpdatedSubject.onNext(contact)
    }

    fun clear() = contactsCache.clear()

    operator fun get(position: Int) = contactsCache[position]

    operator fun get(email: String) = contactsCache.first { it.email == email }

    val size get() = contactsCache.size

    @SuppressLint("CheckResult")
    fun addContactsUpdatedListener(email: String?, contactsUpdatedListener : () -> Unit) {
        contactsUpdatedSubject
                .filter { email == null || it.email == email }
                .subscribe { contactsUpdatedListener() }
    }

    fun addContactsUpdatedListener(contactsUpdatedListener: () -> Unit) =
            addContactsUpdatedListener(null, contactsUpdatedListener)
}
