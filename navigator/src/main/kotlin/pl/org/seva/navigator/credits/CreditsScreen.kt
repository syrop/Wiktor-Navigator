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

package pl.org.seva.navigator.credits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorDimens
import pl.org.seva.navigator.main.ui.NavigatorTheme

private const val DEVELOPER_NAME = "Wiktor Nizio"
private const val LAUNCHER_ICON_AUTHOR = "Zularizal"

/**
 * Jetpack Compose replacement for `res/layout/fr_credits.xml`.
 *
 * Layout mapping:
 * - Root vertical `LinearLayout` (`match_parent`, `padding="16dp"`) -> `Column` with
 *   `Modifier.fillMaxSize().padding(NavigatorDimens.ScreenPadding)`.
 * - App name `TextView` (`48sp`) -> `Text` with `MaterialTheme.typography.displayMedium`.
 * - Section header `TextView`s (`20sp`, `bold`, `marginTop="32dp"` / `16dp`) -> `Text` with
 *   `MaterialTheme.typography.titleMedium` and top padding from `NavigatorDimens`.
 * - Standard `16sp` `TextView`s -> `Text` with `MaterialTheme.typography.bodyLarge`.
 * - Accent-colored interactive `TextView`s (`@color/colorAccent`) -> [CreditsLinkText] using
 *   `MaterialTheme.colorScheme.secondary` and `Modifier.clickable`.
 */
@Composable
fun CreditsScreen(
    versionText: String,
    onDeveloperUrlClick: () -> Unit,
    onCryptoAddressClick: () -> Unit,
    onIconGithubClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(NavigatorDimens.ScreenPadding),
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = versionText,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = stringResource(id = R.string.credits_fragment_developer),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = NavigatorDimens.SectionSpacingLarge),
        )
        Text(
            text = DEVELOPER_NAME,
            style = MaterialTheme.typography.bodyLarge,
        )
        CreditsLinkText(
            text = stringResource(id = R.string.credits_fragment_developer_url_short),
            onClick = onDeveloperUrlClick,
        )
        Text(
            text = stringResource(id = R.string.credits_fragment_developer_donate),
            style = MaterialTheme.typography.bodyLarge,
        )
        CreditsLinkText(
            text = stringResource(id = R.string.credits_fragment_developer_btc),
            onClick = onCryptoAddressClick,
        )

        Text(
            text = stringResource(id = R.string.credits_fragment_launcher_icon_by),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = NavigatorDimens.SectionSpacingMedium),
        )
        Text(
            text = LAUNCHER_ICON_AUTHOR,
            style = MaterialTheme.typography.bodyLarge,
        )
        CreditsLinkText(
            text = stringResource(id = R.string.credits_fragment_icon_github_short),
            onClick = onIconGithubClick,
        )
    }
}

/**
 * Reusable interactive text component representing the clickable `@color/colorAccent`
 * `TextView` links in `fr_credits.xml`.
 */
@Composable
private fun CreditsLinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
fun CreditsScreenPreview() {
    NavigatorTheme {
        Surface {
            CreditsScreen(
                versionText = "ver. 1.0.0",
                onDeveloperUrlClick = {},
                onCryptoAddressClick = {},
                onIconGithubClick = {},
            )
        }
    }
}
