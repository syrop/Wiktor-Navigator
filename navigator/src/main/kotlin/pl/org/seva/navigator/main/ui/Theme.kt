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

package pl.org.seva.navigator.main.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.org.seva.navigator.R

object NavigatorDimens {
    val ScreenPadding = 16.dp
    val SectionSpacingLarge = 32.dp
    val SectionSpacingMedium = 16.dp
}

private val NavigatorTypography = Typography(
    displayMedium = TextStyle(
        fontSize = 48.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
    ),
)

/**
 * Material 3 theme for Navigator Compose screens, mapping XML theme attributes
 * from `res/values/styles.xml` and `res/values/colors.xml`.
 */
@Composable
fun NavigatorTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = lightColorScheme(
        primary = colorResource(id = R.color.colorPrimary),
        primaryContainer = colorResource(id = R.color.colorPrimaryDark),
        secondary = colorResource(id = R.color.colorAccent),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NavigatorTypography,
        content = content,
    )
}
