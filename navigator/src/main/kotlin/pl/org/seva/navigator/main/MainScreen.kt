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

package pl.org.seva.navigator.main

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.appbar.AppBarLayout
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorTheme

/**
 * Jetpack Compose replacement for `res/layout/act_mainr.xml`.
 *
 * Layout mapping:
 * - Root `ConstraintLayout` -> `Column(Modifier.fillMaxSize())`
 * - `AppBarLayout` + `Toolbar` -> `AndroidView` hosting `AppBarLayout`/`Toolbar` (or M3 TopAppBar placeholder in preview)
 * - `fragment` -> `navHostContent` slot hosting `FragmentContainerView(id = R.id.nav_host_fragment)`
 */
@Composable
fun MainScreen(
    onToolbarCreated: (Toolbar) -> Unit,
    modifier: Modifier = Modifier,
    navHostContent: @Composable () -> Unit = {
        AndroidView(
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = R.id.nav_host_fragment
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    },
) {
    val isInspection = LocalInspectionMode.current

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (isInspection) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name_short),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        } else {
            AndroidView(
                factory = { context ->
                    val themedContext = ContextThemeWrapper(context, R.style.AppTheme_AppBarOverlay)
                    AppBarLayout(themedContext).apply {
                        id = R.id.appbar
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        val toolbar = Toolbar(themedContext).apply {
                            id = R.id.toolbar
                            setPopupTheme(R.style.AppTheme_PopupOverlay)
                            val typedArray = themedContext.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
                            val actionBarHeight = typedArray.getDimensionPixelSize(0, ViewGroup.LayoutParams.WRAP_CONTENT)
                            typedArray.recycle()
                            layoutParams = AppBarLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                actionBarHeight,
                            )
                        }
                        addView(toolbar)
                        onToolbarCreated(toolbar)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (isInspection) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "NavHost Content Area",
                        color = Color.Gray,
                    )
                }
            } else {
                navHostContent()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NavigatorTheme {
        Surface {
            MainScreen(
                onToolbarCreated = {},
            )
        }
    }
}
