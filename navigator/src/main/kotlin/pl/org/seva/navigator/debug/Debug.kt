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

package pl.org.seva.navigator.debug

import android.content.SharedPreferences
import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.init.instance
import pl.org.seva.navigator.profile.isLoggedIn
import java.util.*
import java.util.concurrent.TimeUnit

val debug by instance<Debug>()

class Debug(private val prefs: SharedPreferences) {

    val isDebugMode get() = prefs.getBoolean(Debug.PROPERTY, false)

    private var disposable = Disposables.empty()

    fun start() {
        disposable.dispose()
        disposable = Observable.interval(1, TimeUnit.MINUTES)
                .filter { isLoggedIn }
                .subscribe {
                    val cal = Calendar.getInstance()
                    val message = "${cal.get(Calendar.HOUR_OF_DAY)}:" +
                            "${cal.get(Calendar.MINUTE)}:${cal.get(Calendar.SECOND)}"
                    fbWriter.writeDebug(message)
                }
    }

    fun stop() {
        disposable.dispose()
    }

    fun isIgnoredForPeer(
            @Suppress("UNUSED_PARAMETER") peer: String,
            @Suppress("UNUSED_PARAMETER") message: String) = true

    fun withPeerVersionNumber(
            @Suppress("UNUSED_PARAMETER") peer: String,
            message: String) = "0$VERSION_SEPARATOR$message"

    companion object {
        const val PROPERTY = "debug"
        private const val VERSION_SEPARATOR = ":"
    }
}
