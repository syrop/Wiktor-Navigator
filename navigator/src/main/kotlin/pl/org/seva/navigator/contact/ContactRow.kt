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

package pl.org.seva.navigator.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.org.seva.navigator.main.ui.NavigatorTheme

/**
 * Jetpack Compose replacement for `res/layout/row_contact.xml`.
 *
 * Layout mapping:
 * - Root `RelativeLayout` with padding (16dp horizontal, 16dp vertical) -> `Row` with
 *   `Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp)`.
 * - Avatar container with `ImageView` (40dp circle with `contact.color`) and centered `TextView`
 *   (white, 22sp initial letter) -> `Box(Modifier.size(40.dp).background(Color(contact.color), CircleShape))`.
 * - Inner `RelativeLayout` with start padding (72dp total - 16dp start margin - 40dp avatar = 16dp spacing) ->
 *   `Column(Modifier.padding(start = 16.dp))` containing:
 *   - Name `Text` (24sp).
 *   - Email `Text` (20sp).
 */
@Composable
fun ContactRow(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = Color(contact.color), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val initial = if (contact.name.isNotEmpty()) contact.name.substring(0, 1) else ""
            Text(
                text = initial,
                color = Color.White,
                fontSize = 22.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = contact.name,
                fontSize = 24.sp,
            )
            Text(
                text = contact.email,
                fontSize = 20.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactRowPreview() {
    NavigatorTheme {
        Surface {
            ContactRow(
                contact = Contact(
                    name = "Wiktor Nizio",
                    email = "wiktor.nizio@gmail.com",
                    color = android.graphics.Color.BLUE,
                ),
                onClick = {},
            )
        }
    }
}
