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
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import pl.org.seva.navigator.contacts.Contacts
import pl.org.seva.navigator.profile.LoggedInUser
import pl.org.seva.navigator.data.firebase.FbReader
import pl.org.seva.navigator.data.firebase.FbWriter
import pl.org.seva.navigator.data.room.ContactsDatabase
import pl.org.seva.navigator.contacts.FriendshipListener
import pl.org.seva.navigator.contacts.FriendshipSource
import pl.org.seva.navigator.navigation.MyLocationSource
import pl.org.seva.navigator.navigation.PeerLocationSource

fun module(f: KodeinModuleBuilder.() -> Unit) = KodeinModuleBuilder().apply { f() }.build()

inline fun <reified T : Any> instance() = Kodein.global.instance<T>()

class KodeinModuleBuilder {

    lateinit var application: Application

    fun build() = Kodein.Module {
        bind<Bootstrap>() with singleton { Bootstrap(application) }
        bind<FbReader>() with singleton { FbReader() }
        bind<Contacts>() with singleton { Contacts() }
        bind<LoggedInUser>() with singleton { LoggedInUser() }
        bind<FbWriter>() with singleton { FbWriter() }
        bind<FriendshipListener>() with singleton { FriendshipListener() }
        bind<Permissions>() with singleton { Permissions() }
        bind<ActivityRecognitionSource>() with singleton { ActivityRecognitionSource() }
        bind<FriendshipSource>() with singleton { FriendshipSource() }
        bind<PeerLocationSource>() with singleton { PeerLocationSource() }
        bind<MyLocationSource>() with singleton { MyLocationSource() }
        bind<ContactsDatabase>() with singleton { ContactsDatabase() }
        bind<NotificationChannels>() with singleton { NotificationChannels(application) }
        bind<ColorFactory>() with singleton { ColorFactory(application) }
    }
}
