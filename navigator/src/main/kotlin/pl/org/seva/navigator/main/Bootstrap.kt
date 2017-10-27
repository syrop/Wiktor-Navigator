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

package pl.org.seva.navigator.main

import android.app.Application
import android.content.Intent
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import pl.org.seva.navigator.main.service.NavigatorService
import pl.org.seva.navigator.contacts.Contacts
import pl.org.seva.navigator.profile.LoggedInUser
import pl.org.seva.navigator.data.room.ContactsDatabase
import pl.org.seva.navigator.contacts.FriendshipListener
import pl.org.seva.navigator.contacts.FriendshipSource

import pl.org.seva.navigator.data.room.insert

class Bootstrap(private val application: Application) {

    private val contacts: Contacts = instance()
    private val friendshipSource: FriendshipSource = instance()
    private val friendshipListener: FriendshipListener = instance()
    private val contactDao = instance<ContactsDatabase>().contactDao
    private val loggedInUser: LoggedInUser = instance()
    private var isServiceRunning = false

    fun boot() {
        loggedInUser setCurrentUser FirebaseAuth.getInstance().currentUser
        instance<ActivityRecognitionSource>() initGoogleApiClient application
        with(instance<ContactsDatabase>().contactDao) {
            contacts addAll getAll().map { it.contactValue() }
        }
        setDynamicShortcuts(application)
        friendshipListener init application
        if (loggedInUser.isLoggedIn) {
            addFriendshipListeners()
            startService()
        }
        instance<NotificationChannels>().create()
    }

    fun login(user: FirebaseUser) {
        fun downloadFriendsFromCloud() =
                friendshipSource.downloadFriendsFromCloud(
                        onFriendFound = {
                            contacts add it
                            contactDao insert it
                        }, onCompleted = { setDynamicShortcuts(application) })

        loggedInUser setCurrentUser user
        addFriendshipListeners()
        downloadFriendsFromCloud()
        startService()
    }

    fun logout() {
        stopService()
        removeFriendshipListeners()
        contactDao.deleteAll()
        contacts.clear()
        loggedInUser setCurrentUser null
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
