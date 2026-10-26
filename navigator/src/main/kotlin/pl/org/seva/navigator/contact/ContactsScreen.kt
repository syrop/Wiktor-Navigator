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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorTheme

/**
 * Jetpack Compose replacement for `res/layout/fr_contacts.xml`.
 *
 * Layout mapping:
 * - Root `CoordinatorLayout` with FAB -> `Scaffold` with `floatingActionButton = { FloatingActionButton(...) }`
 *   and `snackbarHost = { SnackbarHost(snackbarHostState) }`.
 * - Prompt `TextView` (center, 20sp, bold '+') -> [ContactsEmptyPrompt] aligned to `Alignment.Center`.
 * - `RecyclerView` with vertical dividers and swipe to delete -> `LazyColumn` with [ContactRow],
 *   [HorizontalDivider], and [SwipeToDismissBox].
 */
@Composable
fun ContactsScreen(
    contactsList: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onContactSwipedToDelete: (Contact) -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = stringResource(id = R.string.action_search),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (contactsList.isEmpty()) {
                ContactsEmptyPrompt(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = contactsList,
                        key = { it.email },
                    ) { contact ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if ((dismissValue == SwipeToDismissBoxValue.StartToEnd) ||
                                    (dismissValue == SwipeToDismissBoxValue.EndToStart)
                                ) {
                                    onContactSwipedToDelete(contact)
                                    true
                                } else {
                                    false
                                }
                            },
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Red.copy(alpha = 0.7f)),
                                )
                            },
                            content = {
                                Surface {
                                    ContactRow(
                                        contact = contact,
                                        onClick = { onContactClick(contact) },
                                    )
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsEmptyPrompt(
    modifier: Modifier = Modifier,
) {
    val template = stringResource(id = R.string.contacts_please_press_plus)
    val plusIndex = template.indexOf('+')
    val annotatedString = buildAnnotatedString {
        if (plusIndex >= 0) {
            append(template.substring(0, plusIndex))
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("+")
            }
            append(template.substring(plusIndex + 1))
        } else {
            append(template)
        }
    }

    Text(
        text = annotatedString,
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun ContactsScreenEmptyPreview() {
    NavigatorTheme {
        Surface {
            ContactsScreen(
                contactsList = emptyList(),
                onContactClick = {},
                onContactSwipedToDelete = {},
                onAddContactClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactsScreenListPreview() {
    NavigatorTheme {
        Surface {
            ContactsScreen(
                contactsList = listOf(
                    Contact(
                        name = "Wiktor Nizio",
                        email = "wiktor.nizio@gmail.com",
                        color = android.graphics.Color.BLUE,
                    ),
                    Contact(
                        name = "Alice",
                        email = "alice@example.com",
                        color = android.graphics.Color.MAGENTA,
                    ),
                ),
                onContactClick = {},
                onContactSwipedToDelete = {},
                onAddContactClick = {},
            )
        }
    }
}
