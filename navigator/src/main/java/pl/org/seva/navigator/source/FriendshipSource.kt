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

package pl.org.seva.navigator.source

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.disposables.CompositeDisposable
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.database.firebase.FirebaseReader
import pl.org.seva.navigator.presenter.FriendshipListener

@Singleton
class FriendshipSource @Inject internal constructor() {

    @Inject
    lateinit var firebaseReader: FirebaseReader

    private val compositeDisposable = CompositeDisposable()

    fun addFriendshipListener(friendshipListener: FriendshipListener) {
        compositeDisposable.addAll(
                firebaseReader
                        .friendshipRequestedListener()
                        .subscribe{ friendshipListener.onPeerRequestedFriendship(it) },
                firebaseReader
                        .friendshipAcceptedListener()
                        .subscribe { friendshipListener.onPeerAcceptedFriendship(it) },
                firebaseReader
                        .friendshipDeletedListener()
                        .subscribe { friendshipListener.onPeerDeletedFriendship(it) })
    }

    fun downloadFriendsFromServer(listener: (Contact) -> Unit) {
        firebaseReader.readFriendsOnce().subscribe { listener(it) }
    }

    fun clearFriendshipListeners() {
        compositeDisposable.clear()
    }
}
