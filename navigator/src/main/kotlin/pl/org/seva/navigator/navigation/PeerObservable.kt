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

package pl.org.seva.navigator.navigation

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import pl.org.seva.navigator.main.data.fb.fbReader
import pl.org.seva.navigator.main.init.instance

val peerObservable by instance<PeerObservable>()

class PeerObservable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun addLocationObserver(email: String, f: (latLng: LatLng) -> Unit) {
        scope.launch {
            fbReader.peerLocationListener(email).collect { f(it) }
        }
    }

    fun addDebugObserver(email: String, f: (String) -> Unit) {
        scope.launch {
            fbReader.debugListener(email).collect { f(it) }
        }
    }

    fun clearPeerListeners() = scope.coroutineContext.cancelChildren()
}
