/*
 * Copyright (C) 2026 Wiktor Nizio
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

package pl.org.seva.navigator.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import pl.org.seva.navigator.debug.Debug
import pl.org.seva.navigator.debug.debug
import pl.org.seva.navigator.main.extension.prefs

@Composable
fun SettingsRoute() {
    val context = LocalContext.current
    var isDebugMode by remember { mutableStateOf(value = debug.isDebugMode) }

    SettingsScreen(
        isDebugMode = isDebugMode,
        onDebugModeChanged = { enabled ->
            isDebugMode = enabled
            context.prefs.edit {
                putBoolean(Debug.PROPERTY, enabled)
            }
            if (enabled) {
                debug.start()
            } else {
                debug.stop()
            }
        },
    )
}
