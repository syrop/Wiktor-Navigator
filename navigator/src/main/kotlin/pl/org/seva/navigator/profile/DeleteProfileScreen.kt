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

package pl.org.seva.navigator.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorTheme

/**
 * Jetpack Compose replacement for `res/layout/fr_delete_profile.xml`.
 *
 * Layout mapping:
 * - Root `ConstraintLayout` -> `Column` containing:
 *   - Centered description text inside a weighted/scrollable `Box` (`20sp`, with `16dp` horizontal and `8dp` vertical padding).
 *   - Bottom `Row` containing two equal-weighted (`weight(1f)`) borderless buttons (`TextButton`
 *     corresponding to `?android:attr/buttonBarButtonStyle`), spaced `8dp` apart.
 */
@Composable
fun DeleteProfileScreen(
    onCancelClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.delete_description),
                fontSize = 20.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(id = R.string.delete_user_cancel),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            TextButton(
                onClick = onOkClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(id = R.string.delete_user_ok),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteProfileScreenPreview() {
    NavigatorTheme {
        Surface {
            DeleteProfileScreen(
                onCancelClick = {},
                onOkClick = {},
            )
        }
    }
}
