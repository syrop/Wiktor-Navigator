/*
 * Copyright (C) 2018 Wiktor Nizio
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

package pl.org.seva.navigator.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

infix fun <T> Context.start(clazz: Class<T>): Boolean {
    startActivity(Intent(this, clazz))
    return true
}

fun <T> Context.start(clazz: Class<T>, f: Intent.() -> Intent): Boolean {
    startActivity(Intent(this, clazz).run(f))
    return true
}

val prefs by instance<SharedPreferences>()

val applicationContext by instance<Context>()

val appContext by instance<Context>()

val versionName: String by lazy {
    appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
}

fun <T> LiveData<T>.observe(owner: LifecycleOwner, f: (T) -> Unit) =
        observe(owner, Observer<T> { f(it) })
