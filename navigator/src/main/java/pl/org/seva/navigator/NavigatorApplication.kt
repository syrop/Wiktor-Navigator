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
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.google.firebase.auth.FirebaseUser
import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.Login
import pl.org.seva.navigator.model.firebase.FbReader
import pl.org.seva.navigator.model.firebase.FbWriter
import pl.org.seva.navigator.model.sqlite.SqlReader
import pl.org.seva.navigator.model.sqlite.SqlWriter
import pl.org.seva.navigator.presenter.FriendshipListener
import pl.org.seva.navigator.presenter.Permissions
import pl.org.seva.navigator.source.ActivityRecognitionSource
import pl.org.seva.navigator.source.FriendshipSource
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.source.PeerLocationSource

class NavigatorApplication: Application(), KodeinGlobalAware {

    private val bootstrap: Bootstrap get() = instance()

    private val navigatorModule = Kodein.Module {
        bind<Bootstrap>() with singleton { Bootstrap(this@NavigatorApplication) }
        bind<FbReader>() with singleton { FbReader() }
        bind<ContactsStore>() with singleton { ContactsStore() }
        bind<Login>() with singleton { Login() }
        bind<FbWriter>() with singleton { FbWriter() }
        bind<SqlReader>() with singleton { SqlReader() }
        bind<SqlWriter>() with singleton { SqlWriter() }
        bind<FriendshipListener>() with singleton { FriendshipListener() }
        bind<Permissions>() with singleton { Permissions() }
        bind<ActivityRecognitionSource>() with singleton { ActivityRecognitionSource() }
        bind<FriendshipSource>() with singleton { FriendshipSource() }
        bind<PeerLocationSource>() with singleton { PeerLocationSource() }
        bind<MyLocationSource>() with singleton { MyLocationSource() }
    }

    init {
        Kodein.global.addImport(navigatorModule)
    }

    override fun onCreate() {
        super.onCreate()
        bootstrap.boot()
    }

    fun login(user: FirebaseUser) = bootstrap.login(user)
    fun logout() = bootstrap.logout()
    fun stopService() = bootstrap.stopService()
}
