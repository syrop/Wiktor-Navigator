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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorTheme

/**
 * Jetpack Compose replacement for `res/layout/fr_settings.xml` and `res/xml/preferences.xml`.
 *
 * Provides the settings interface allowing the user to toggle debug mode,
 * including showing the debug information confirmation dialog when enabled.
 */
@Composable
fun SettingsScreen(
    isDebugMode: Boolean,
    onDebugModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDebugInfoDialog by remember { mutableStateOf(value = false) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newChecked = !isDebugMode
                    onDebugModeChanged(newChecked)
                    if (newChecked) {
                        showDebugInfoDialog = true
                    }
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.pref_debug_mode),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Checkbox(
                checked = isDebugMode,
                onCheckedChange = { newChecked ->
                    onDebugModeChanged(newChecked)
                    if (newChecked) {
                        showDebugInfoDialog = true
                    }
                },
            )
        }
    }

    if (showDebugInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDebugInfoDialog = false },
            title = {
                Text(text = stringResource(id = R.string.pref_debug_mode))
            },
            text = {
                Text(text = stringResource(id = R.string.settings_fragment_debug_info))
            },
            confirmButton = {
                TextButton(
                    onClick = { showDebugInfoDialog = false },
                ) {
                    Text(
                        text = stringResource(id = android.R.string.ok),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NavigatorTheme {
        Surface {
            SettingsScreen(
                isDebugMode = true,
                onDebugModeChanged = {},
            )
        }
    }
}
