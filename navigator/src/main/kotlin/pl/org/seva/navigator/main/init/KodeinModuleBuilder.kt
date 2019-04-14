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
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.kodein.di.Kodein
import org.kodein.di.conf.global
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import pl.org.seva.navigator.contact.Contacts
import pl.org.seva.navigator.profile.LoggedInUser
import pl.org.seva.navigator.main.model.fb.FbReader
import pl.org.seva.navigator.main.model.fb.FbWriter
import pl.org.seva.navigator.main.model.db.ContactsDatabase
import pl.org.seva.navigator.contact.FriendshipListener
import pl.org.seva.navigator.contact.FriendshipObservable
import pl.org.seva.navigator.main.model.db.ContactDao
import pl.org.seva.navigator.main.model.db.db
import pl.org.seva.navigator.debug.Debug
import pl.org.seva.navigator.main.model.ActivityRecognitionObservable
import pl.org.seva.navigator.main.model.Permissions
import pl.org.seva.navigator.main.view.Toaster
import pl.org.seva.navigator.main.view.ColorFactory
import pl.org.seva.navigator.main.view.NotificationChannels
import pl.org.seva.navigator.navigation.MyLocationObservable
import pl.org.seva.navigator.navigation.PeerObservable

val Context.module get() = KodeinModuleBuilder(this).build()

inline fun <reified R : Any> instance() = Kodein.global.instance<R>()

class KodeinModuleBuilder(private val ctx: Context) {

    fun build() = Kodein.Module("main") {
        bind<Context>() with provider { ctx }
        bind<SharedPreferences>() with singleton { PreferenceManager.getDefaultSharedPreferences(ctx) }
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
        bind<Debug>() with singleton { Debug() }
        bind<ContactDao>() with singleton { db.contactDao }
        bind<Toaster>() with singleton { Toaster() }
    }
}
