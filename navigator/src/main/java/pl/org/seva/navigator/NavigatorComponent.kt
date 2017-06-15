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

import javax.inject.Singleton

import dagger.Component
import pl.org.seva.navigator.presenter.NavigatorService
import pl.org.seva.navigator.view.activity.ContactsActivity
import pl.org.seva.navigator.view.activity.LoginActivity
import pl.org.seva.navigator.view.activity.NavigationActivity
import pl.org.seva.navigator.view.activity.SearchActivity
import pl.org.seva.navigator.view.adapter.ContactAdapter

@Singleton
@Component(modules = arrayOf(pl.org.seva.navigator.NavigatorModule::class))
interface NavigatorComponent {
    fun inject(navigatorApplication: NavigatorApplication)
    fun inject(loginActivity: LoginActivity)
    fun inject(searchActivity: SearchActivity)
    fun inject(contactAdapter: ContactAdapter)
    fun inject(ca: ContactsActivity)
    fun inject(navigationActivity: NavigationActivity)
    fun inject(navigatorService: NavigatorService)
}
