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
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.init.instance
import pl.org.seva.navigator.profile.isLoggedIn

val debug by instance<Debug>()

class Debug(private val prefs: SharedPreferences) {

    val isDebugMode get() = prefs.getBoolean(Debug.PROPERTY, false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(1.minutes)
                if (isLoggedIn) {
                    val cal = Calendar.getInstance()
                    val message = "${cal.get(Calendar.HOUR_OF_DAY)}:" +
                            "${cal.get(Calendar.MINUTE)}:${cal.get(Calendar.SECOND)}"
                    fbWriter.writeDebug(message)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
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
