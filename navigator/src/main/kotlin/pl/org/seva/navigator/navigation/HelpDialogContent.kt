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

package pl.org.seva.navigator.navigation

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import pl.org.seva.navigator.main.ui.NavigatorTheme

private const val UTF_8 = "utf-8"
private const val PLAIN_TEXT = "text/html"
private const val ASSET_DIR = "file:///android_asset/"

/**
 * Jetpack Compose replacement for `res/layout/dialog_help.xml`.
 *
 * Layout mapping:
 * - Root `RelativeLayout` with `padding="8dp"` -> `Column(Modifier.fillMaxSize().padding(8.dp))`.
 * - `WebView` filling space above action button -> `AndroidView(factory = { WebView(it) })`
 *   with `Modifier.weight(1f).fillMaxWidth()`.
 * - Bottom action button (`style="?android:attr/buttonBarButtonStyle"`) -> `TextButton(Modifier.fillMaxWidth())`.
 */
@Composable
fun HelpDialogContent(
    htmlContent: String,
    actionButtonText: String,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        if (LocalInspectionMode.current) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Help content preview",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.defaultTextEncodingName = UTF_8
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(ASSET_DIR, htmlContent, PLAIN_TEXT, UTF_8, null)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }

        TextButton(
            onClick = onActionButtonClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = actionButtonText,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HelpDialogContentPreview() {
    NavigatorTheme {
        Surface {
            HelpDialogContent(
                htmlContent = "<html><body><h1>Help</h1><p>Test content</p></body></html>",
                actionButtonText = "OK",
                onActionButtonClick = {},
            )
        }
    }
}
