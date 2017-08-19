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
import android.content.Context
import android.content.Intent
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.with
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.Login
import pl.org.seva.navigator.model.room.ContactsDatabase
import pl.org.seva.navigator.presenter.FriendshipListener
import pl.org.seva.navigator.source.ActivityRecognitionSource
import pl.org.seva.navigator.source.FriendshipSource
import pl.org.seva.navigator.view.builder.notification.Channels

class Bootstrap(private val application: Application) : KodeinGlobalAware {

    private val contactsStore: ContactsStore = instance()
    private val friendshipSource: FriendshipSource = instance()
    private val friendshipListener: FriendshipListener = instance()
    private val login: Login = instance()
    private var isServiceRunning = false

    fun boot() {
        login.setCurrentUser(FirebaseAuth.getInstance().currentUser)
        instance<ActivityRecognitionSource>().initGoogleApiClient(application)
        with(instance<ContactsDatabase>().contactDao) {
            contactsStore.addAll(getAll())
        }
        friendshipListener.init(application)
        if (login.isLoggedIn) {
            addFriendshipListeners()
            startService()
        }
        with<Context>(application).instance<Channels>().create()
    }

    fun login(user: FirebaseUser) {
        login.setCurrentUser(user)
        addFriendshipListeners()
        downloadFriendsFromCloud()
        startService()
    }

    fun logout() {
        stopService()
        removeFriendshipListeners()
        contactsStore.clear()
        login.setCurrentUser(null)
    }

    private fun addFriendshipListeners() = friendshipSource.addFriendshipListener(friendshipListener)

    private fun downloadFriendsFromCloud() =
        friendshipSource.downloadFriendsFromCloud {
            contactsStore.add(it)
            instance<ContactsDatabase>().contactDao.insert(it)
        }

    private fun removeFriendshipListeners() = friendshipSource.clearFriendshipListeners()

    private fun startService() {
        if (isServiceRunning) {
            return
        }
        application.startService(Intent(application.baseContext, NavigatorService::class.java))
        isServiceRunning = true
    }

    fun stopService() {
        if (!isServiceRunning) {
            return
        }
        application.stopService(Intent(application.baseContext, NavigatorService::class.java))
        isServiceRunning = false
    }
}
