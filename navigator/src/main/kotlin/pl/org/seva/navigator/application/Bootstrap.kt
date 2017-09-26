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

package pl.org.seva.navigator.application

import android.app.Application
import android.content.Intent
import android.os.Build
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import pl.org.seva.navigator.application.service.NavigatorService
import pl.org.seva.navigator.data.ContactsStore
import pl.org.seva.navigator.data.Login
import pl.org.seva.navigator.data.room.ContactsDatabase
import pl.org.seva.navigator.data.room.entity.ContactEntity
import pl.org.seva.navigator.friendship.FriendshipListener
import pl.org.seva.navigator.friendship.FriendshipSource
import pl.org.seva.navigator.ui.notification.Channels

class Bootstrap(private val application: Application) : KodeinGlobalAware {

    private val contactsStore: ContactsStore = instance()
    private val friendshipSource: FriendshipSource = instance()
    private val friendshipListener: FriendshipListener = instance()
    private val contactDao = instance<ContactsDatabase>().contactDao
    private val login: Login = instance()
    private var isServiceRunning = false

    fun boot() {
        login.setCurrentUser(FirebaseAuth.getInstance().currentUser)
        instance<ActivityRecognitionSource>().initGoogleApiClient(application)
        with(instance<ContactsDatabase>().contactDao) {
            contactsStore.addAll(getAll().map { it.contactValue() })
        }
        setDynamicShortcuts(application)
        friendshipListener.init(application)
        if (login.isLoggedIn) {
            addFriendshipListeners()
            startService()
        }
        instance<Channels>().create()
    }

    fun login(user: FirebaseUser) {
        fun downloadFriendsFromCloud() =
                friendshipSource.downloadFriendsFromCloud(
                        onFriendFound = {
                            contactsStore.add(it)
                            contactDao.insert(ContactEntity(it))
                        }, onCompleted = { setDynamicShortcuts(application) })

        login.setCurrentUser(user)
        addFriendshipListeners()
        downloadFriendsFromCloud()
        startService()
    }

    fun logout() {
        stopService()
        removeFriendshipListeners()
        contactDao.deleteAll()
        contactsStore.clear()
        login.setCurrentUser(null)
        setDynamicShortcuts(application)
    }

    private fun addFriendshipListeners() = friendshipSource.addFriendshipListener(friendshipListener)

    private fun removeFriendshipListeners() = friendshipSource.clearFriendshipListeners()

    fun startService() {
        if (isServiceRunning) {
            return
        }
        startService(Intent(application.baseContext, NavigatorService::class.java))
        isServiceRunning = true
    }

    private fun startService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    fun stopService() {
        if (!isServiceRunning) {
            return
        }
        application.stopService(Intent(application.baseContext, NavigatorService::class.java))
        isServiceRunning = false
    }
}
