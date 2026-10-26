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

package pl.org.seva.navigator.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import pl.org.seva.navigator.debug.Debug
import pl.org.seva.navigator.debug.debug
import pl.org.seva.navigator.main.extension.prefs
import pl.org.seva.navigator.main.ui.NavigatorTheme

class SettingsFragmentContainer : Fragment() {

    private val isDebugMode = mutableStateOf(value = false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        isDebugMode.value = debug.isDebugMode
        setContent {
            NavigatorTheme {
                SettingsScreen(
                    isDebugMode = isDebugMode.value,
                    onDebugModeChanged = { enabled ->
                        isDebugMode.value = enabled
                        requireContext().prefs.edit {
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
        }
    }
}
