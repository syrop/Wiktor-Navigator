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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorTheme

enum class SeekPromptType {
    PRESS_TO_BEGIN,
    NO_USER_FOUND,
}

sealed interface SeekContactUiState {
    data class Prompt(val type: SeekPromptType) : SeekContactUiState
    data class ContactResult(val contact: Contact) : SeekContactUiState
    data object Searching : SeekContactUiState
    data object Hidden : SeekContactUiState
}

private const val SEARCH_ICON_ID = "search_icon"
private const val IMAGE_PLACEHOLDER = "[image]"

/**
 * Jetpack Compose replacement for `res/layout/fr_seek_contact.xml`.
 *
 * Layout mapping:
 * - Root `RelativeLayout` -> `Box(Modifier.fillMaxSize())`
 * - Center prompt `TextView` (`20sp`) with inline search icon or no-user text ->
 *   [SeekPromptText] aligned to `Alignment.Center`.
 * - Results `RecyclerView` -> `LazyColumn` rendering [ContactRow].
 * - Loading indicator during search -> [CircularProgressIndicator].
 * - Friend confirmation dialog -> [AlertDialog].
 */
@Composable
fun SeekContactScreen(
    uiState: SeekContactUiState,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    confirmationContact: Contact? = null,
    onConfirmAddContact: (Contact) -> Unit = {},
    onDismissConfirmation: () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (uiState) {
            is SeekContactUiState.Prompt -> {
                SeekPromptText(
                    type = uiState.type,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                )
            }
            is SeekContactUiState.Searching -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            is SeekContactUiState.ContactResult -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ContactRow(
                            contact = uiState.contact,
                            onClick = { onContactClick(uiState.contact) },
                        )
                    }
                }
            }
            SeekContactUiState.Hidden -> {
                // Invisible when searching/filtering
            }
        }

        if (confirmationContact != null) {
            AlertDialog(
                onDismissRequest = onDismissConfirmation,
                title = {
                    Text(text = stringResource(id = R.string.seek_contact_adding_friend_title))
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.add_friend_confirmation)
                            .replace("[name]", confirmationContact.name),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onConfirmAddContact(confirmationContact) }) {
                        Text(text = stringResource(id = android.R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissConfirmation) {
                        Text(text = stringResource(id = android.R.string.no))
                    }
                },
            )
        }
    }
}

@Composable
private fun SeekPromptText(
    type: SeekPromptType,
    modifier: Modifier = Modifier,
) {
    when (type) {
        SeekPromptType.NO_USER_FOUND -> {
            Text(
                text = stringResource(id = R.string.seek_contact_no_user_found),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = modifier,
            )
        }
        SeekPromptType.PRESS_TO_BEGIN -> {
            val template = stringResource(id = R.string.seek_contact_press_to_begin)
            val placeholderIndex = template.indexOf(IMAGE_PLACEHOLDER)

            if (placeholderIndex >= 0) {
                val before = template.substring(0, placeholderIndex)
                val after = template.substring(placeholderIndex + IMAGE_PLACEHOLDER.length)
                val annotatedString = buildAnnotatedString {
                    append(before)
                    appendInlineContent(SEARCH_ICON_ID, "[search]")
                    append(after)
                }

                val inlineContent = mapOf(
                    SEARCH_ICON_ID to InlineTextContent(
                        Placeholder(
                            width = 24.sp,
                            height = 24.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_black_24dp),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                )

                Text(
                    text = annotatedString,
                    inlineContent = inlineContent,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = modifier,
                )
            } else {
                Text(
                    text = template,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = modifier,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SeekContactScreenPromptPreview() {
    NavigatorTheme {
        Surface {
            SeekContactScreen(
                uiState = SeekContactUiState.Prompt(SeekPromptType.PRESS_TO_BEGIN),
                onContactClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SeekContactScreenResultPreview() {
    NavigatorTheme {
        Surface {
            SeekContactScreen(
                uiState = SeekContactUiState.ContactResult(
                    Contact(
                        name = "Wiktor Nizio",
                        email = "wiktor.nizio@gmail.com",
                        color = android.graphics.Color.BLUE,
                    )
                ),
                onContactClick = {},
            )
        }
    }
}
