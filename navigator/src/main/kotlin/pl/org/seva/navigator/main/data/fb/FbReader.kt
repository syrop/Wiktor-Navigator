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

package pl.org.seva.navigator.main.data.fb

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.debug.debug
import pl.org.seva.navigator.main.init.instance
import pl.org.seva.navigator.main.ui.colorFactory

val fbReader by instance<FbReader>()

class FbReader : Fb() {

    fun peerLocationListener(email: String): Flow<LatLng> {
        return email.toReference().child(LAT_LNG).listen()
                .filter { it.value != null }
                .map { checkNotNull(it.value) as String }
                .map { it.toLatLng() }
    }

    fun debugListener(email: String): Flow<String> {
        return email.toReference().child(DEBUG).listen()
                .filter { it.value != null }
                .map { checkNotNull(it.value) as String }
                .filter { !debug.isIgnoredForPeer(email, it) }
    }

    fun friendshipRequestedListener(): Flow<Contact> = FRIENDSHIP_REQUESTED.contactListener()

    fun friendshipAcceptedListener(): Flow<Contact> = FRIENDSHIP_ACCEPTED.contactListener()

    fun friendshipDeletedListener(): Flow<Contact> = FRIENDSHIP_DELETED.contactListener()

    fun readFriends(): Flow<Contact> = flow {
        val reference = currentUserReference().child(FRIENDS)
        val snapshot = reference.read()
        for (child in snapshot.children) {
            if (child.exists()) {
                emit(child.toContact())
            }
        }
    }

    suspend fun findContact(email: String): Contact = email.toReference().readContact()

    private suspend fun DatabaseReference.readContact(): Contact = read().toContact()

    private fun String.contactListener(): Flow<Contact> = flow {
        val reference = currentUserReference().child(this@contactListener)
        val initialSnapshot = reference.read()
        for (child in initialSnapshot.children) {
            reference.child(checkNotNull(child.key)).removeValue()
            emit(child.toContact())
        }
        emitAll(
            reference.childListener()
                .onEach { reference.child(checkNotNull(it.key)).removeValue() }
                .map { it.toContact() }
        )
    }

    private fun DatabaseReference.childListener(): Flow<DataSnapshot> = callbackFlow {
        val listener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                trySend(dataSnapshot)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onChildRemoved(dataSnapshot: DataSnapshot) = Unit

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(databaseError: DatabaseError) = Unit
        }
        addChildEventListener(listener)
        awaitClose { removeEventListener(listener) }
    }

    private suspend fun DatabaseReference.read(): DataSnapshot =
        suspendCancellableCoroutine { continuation ->
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (continuation.isActive) {
                        continuation.resume(dataSnapshot)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception(databaseError.message))
                    }
                }
            }
            addListenerForSingleValueEvent(listener)
            continuation.invokeOnCancellation { removeEventListener(listener) }
        }

    private fun DatabaseReference.listen(): Flow<DataSnapshot> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                trySend(dataSnapshot)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                close(Exception(databaseError.message))
            }
        }
        addValueEventListener(listener)
        awaitClose { removeEventListener(listener) }
    }

    private fun DataSnapshot.toContact() =
            if (exists()) {
                Contact(checkNotNull(key).from64(), child(DISPLAY_NAME).value as String, colorFactory.nextColor())
            } else Contact()
}
