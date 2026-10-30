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

import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import pl.org.seva.navigator.main.data.fb.fbReader
import pl.org.seva.navigator.main.init.instance

val contacts by instance<Contacts>()

private val contactsScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

fun downloadFriendsFromFb(onFriendFound: (Contact) -> Unit, onCompleted: () -> Unit): Job =
        contactsScope.launch {
            fbReader.readFriends()
                    .onCompletion { cause -> if (cause == null) onCompleted() }
                    .collect { onFriendFound(it) }
        }

class Contacts {

    private val contactsCache = ArrayList<Contact>()
    private val contactsUpdatedFlow = MutableSharedFlow<Contact>(extraBufferCapacity = 1)

    operator fun contains(contact: Contact) = contactsCache.contains(contact)

    fun snapshot() = ArrayList(contactsCache)

    infix fun add(contact: Contact) {
        if (contactsCache.contains(contact)) {
            return
        }
        contactsCache.add(contact)
        contactsCache.sort()
        contactsUpdatedFlow.tryEmit(contact)
    }

    infix fun addAll(contacts: Collection<Contact>) {
        this.contactsCache.addAll(contacts)
        this.contactsCache.sort()
    }

    infix fun delete(contact: Contact) {
        contactsCache.remove(contact)
        contactsUpdatedFlow.tryEmit(contact)
    }

    fun clear() = contactsCache.clear()

    operator fun get(position: Int) = contactsCache[position]

    operator fun get(email: String) = contactsCache.first { it.email == email }

    val size get() = contactsCache.size

    fun addContactsUpdatedListener(email: String?, contactsUpdatedListener: () -> Unit): Job =
            contactsScope.launch {
                contactsUpdatedFlow
                        .filter { email == null || it.email == email }
                        .collect { contactsUpdatedListener() }
            }

    fun addContactsUpdatedListener(contactsUpdatedListener: () -> Unit): Job =
            addContactsUpdatedListener(null, contactsUpdatedListener)
}
