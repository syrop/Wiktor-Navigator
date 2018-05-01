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
 */

package pl.org.seva.navigator.debug

import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import pl.org.seva.navigator.data.fb.fbWriter
import pl.org.seva.navigator.main.instance
import pl.org.seva.navigator.main.prefs
import pl.org.seva.navigator.profile.loggedInUser
import java.util.*
import java.util.concurrent.TimeUnit

fun debug() = instance<Debug>()

fun isDebugMode() = prefs().getBoolean(Debug.PROPERTY, false)

class Debug {

    private val loggedInUser = loggedInUser()

    private var disposable = Disposables.empty()

    fun start() {
        disposable.dispose()  // Idempotent operation (can be "disposed" if already disposed).
        disposable = Observable.interval(1, TimeUnit.MINUTES)
                .filter { loggedInUser.isLoggedIn }
                .subscribe {
                    val cal = Calendar.getInstance()
                    val message = "${cal.get(Calendar.HOUR_OF_DAY)}:" +
                            "${cal.get(Calendar.MINUTE)}:${cal.get(Calendar.SECOND)}"
                    fbWriter().debug(message)
                }
    }

    fun stop() {
        // Dispose operation is idempotent. Calling it again is ignored.
        disposable.dispose()
    }

    companion object {
        const val PROPERTY = "debug"
    }
}
