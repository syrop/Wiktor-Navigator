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

package pl.org.seva.navigator

import android.app.Application

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

import javax.inject.Inject

import pl.org.seva.navigator.model.database.sqlite.DbHelper
import pl.org.seva.navigator.model.database.sqlite.SqliteReader
import pl.org.seva.navigator.model.database.sqlite.SqliteWriter
import pl.org.seva.navigator.presenter.FriendshipListener
import pl.org.seva.navigator.presenter.MyLocationListener
import pl.org.seva.navigator.source.ActivityRecognitionSource
import pl.org.seva.navigator.model.ContactsCache
import pl.org.seva.navigator.source.FriendshipSource
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.model.Contact

class NavigatorApplication : Application() {

    @Inject
    lateinit var activityRecognitionSource: ActivityRecognitionSource
    @Inject
    lateinit var sqliteWriter: SqliteWriter
    @Inject
    lateinit var sqliteReader: SqliteReader
    @Inject
    lateinit var contactsCache: ContactsCache
    @Inject
    lateinit var myLocationSource: MyLocationSource
    @Inject
    lateinit var myLocationListener: MyLocationListener
    @Inject
    lateinit var friendshipSource: FriendshipSource
    @Inject
    lateinit var friendshipListener: FriendshipListener

    lateinit var graph: NavigatorComponent

    override fun onCreate() {
        super.onCreate()
        graph = createGraph()
        graph.inject(this)
        setCurrentUser(FirebaseAuth.getInstance().currentUser)
        activityRecognitionSource.init(this)
        val helper = DbHelper(this)
        sqliteWriter.setHelper(helper)
        sqliteReader.setHelper(helper)
        contactsCache.addAll(sqliteReader.friends)
        myLocationSource.init(this).addLocationListener(myLocationListener)
        friendshipListener.init(this)
        if (isLoggedIn) {
            setFriendshipListeners()
        }
    }

    private fun createGraph(): NavigatorComponent {
        return DaggerNavigatorComponent.create()
    }

    fun login(user: FirebaseUser) {
        setCurrentUser(user)
        setFriendshipListeners()
    }

    fun logout() {
        clearFriendshipListeners()
        setCurrentUser(null)
    }

    private fun setFriendshipListeners() {
        friendshipSource.addFriendshipListener(friendshipListener)
    }

    private fun clearFriendshipListeners() {
        friendshipSource.clearFriendshipListeners()
    }

    companion object {

        var isLoggedIn: Boolean = false
        var email: String? = null
        var displayName: String? = null

        val loggedInContact: Contact
            get() = Contact().setEmail(email!!).setName(displayName!!)

        private fun setCurrentUser(user: FirebaseUser?) {
            if (user != null) {
                isLoggedIn = true
                email = user.email
                displayName = user.displayName
            } else {
                isLoggedIn = false
                email = null
                displayName = null
            }
        }
    }
}
