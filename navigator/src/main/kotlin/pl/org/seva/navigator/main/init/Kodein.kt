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

package pl.org.seva.navigator.main.init

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.kodein.di.*
import pl.org.seva.navigator.contact.Contacts
import pl.org.seva.navigator.profile.LoggedInUser
import pl.org.seva.navigator.main.data.fb.FbReader
import pl.org.seva.navigator.main.data.fb.FbWriter
import pl.org.seva.navigator.main.data.db.ContactsDatabase
import pl.org.seva.navigator.contact.FriendshipListener
import pl.org.seva.navigator.contact.FriendshipObservable
import pl.org.seva.navigator.main.data.db.ContactDao
import pl.org.seva.navigator.main.data.db.db
import pl.org.seva.navigator.debug.Debug
import pl.org.seva.navigator.main.data.ActivityRecognitionObservable
import pl.org.seva.navigator.main.data.Permissions
import pl.org.seva.navigator.main.data.appContext
import pl.org.seva.navigator.main.extension.prefs
import pl.org.seva.navigator.main.ui.ColorFactory
import pl.org.seva.navigator.main.ui.NotificationChannels
import pl.org.seva.navigator.navigation.MyLocationObservable
import pl.org.seva.navigator.navigation.PeerObservable

lateinit var kodein : DI

inline fun <reified R : Any> instance(tag: Any? = null) = kodein.instance<R>(tag)

fun createKodein(ctx: Context) {
    kodein = DI {
        bind<Context>() with provider { ctx }
        bind<Bootstrap>() with singleton { Bootstrap() }
        bind<FusedLocationProviderClient>() with singleton {
            LocationServices.getFusedLocationProviderClient(ctx)
        }
        bind<FbReader>() with singleton { FbReader() }
        bind<FbWriter>() with singleton { FbWriter() }
        bind<Contacts>() with singleton { Contacts() }
        bind<LoggedInUser>() with singleton { LoggedInUser() }
        bind<FriendshipListener>() with singleton { FriendshipListener() }
        bind<Permissions>() with singleton { Permissions() }
        bind<ActivityRecognitionObservable>() with singleton { ActivityRecognitionObservable() }
        bind<FriendshipObservable>() with singleton { FriendshipObservable() }
        bind<PeerObservable>() with singleton { PeerObservable() }
        bind<MyLocationObservable>() with singleton { MyLocationObservable() }
        bind<ContactsDatabase>() with singleton { ContactsDatabase() }
        bind<NotificationChannels>() with singleton { NotificationChannels() }
        bind<ColorFactory>() with singleton { ColorFactory() }
        bind<Debug>() with singleton { Debug(ctx.prefs) }
        bind<ContactDao>() with singleton { db.contactDao }
        bind<String>(APP_VERSION) with singleton { ctx.packageManager.getPackageInfo(appContext.packageName, 0).versionName }
    }
}

const val APP_VERSION = "app_version"
