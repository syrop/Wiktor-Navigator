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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.app_name_short),
    canNavigateBack: Boolean = false,
    onNavigateUp: () -> Unit = {},
    currentRoute: String = NavigatorRoutes.NAVIGATION,
    isLocationPermissionGranted: Boolean = true,
    isUserLoggedIn: Boolean = false,
    onHelpClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    onSearchQuerySubmitted: (String) -> Unit = {},
    navHostContent: @Composable () -> Unit = {},
) {
    val isInspection = LocalInspectionMode.current
    var showOverflowMenu by remember { mutableStateOf(value = false) }
    var searchText by remember { mutableStateOf(value = "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopAppBar(
            title = {
                if (currentRoute == NavigatorRoutes.SEEK_CONTACT && isSearchActive) {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.seek_contact_fragment_hint),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchText.isNotBlank()) {
                                    onSearchQuerySubmitted(searchText)
                                    onSearchActiveChange(false)
                                }
                            },
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            navigationIcon = {
                if (canNavigateBack) {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                searchText = ""
                                onSearchActiveChange(false)
                            } else {
                                onNavigateUp()
                            }
                        },
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            },
            actions = {
                when (currentRoute) {
                    NavigatorRoutes.NAVIGATION -> {
                        if (!isLocationPermissionGranted || !isUserLoggedIn) {
                            IconButton(onClick = onHelpClick) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_help_white_24dp),
                                    contentDescription = stringResource(id = R.string.action_help),
                                )
                            }
                        }
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Text(
                                text = "⋮",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            if (isUserLoggedIn) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.action_logout)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onLogoutClick()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.action_delete_user)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onDeleteAccountClick()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.action_settings)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onSettingsClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.action_credits)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onCreditsClick()
                                },
                            )
                        }
                    }
                    NavigatorRoutes.SEEK_CONTACT -> {
                        if (isSearchActive) {
                            IconButton(
                                onClick = {
                                    if (searchText.isNotEmpty()) {
                                        searchText = ""
                                    } else {
                                        onSearchActiveChange(false)
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close_black_24dp),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    searchText = ""
                                    onSearchActiveChange(true)
                                },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search_white_24dp),
                                    contentDescription = stringResource(id = R.string.action_search),
                                )
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

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
            MainScreen()
        }
    }
}
