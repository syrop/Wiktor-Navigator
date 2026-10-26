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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.NavigatorTheme

class SeekContactScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun seekContactScreen_displaysPromptWhenAwaitingSearch() {
        composeTestRule.setContent {
            NavigatorTheme {
                SeekContactScreen(
                    uiState = SeekContactUiState.Prompt(SeekPromptType.PRESS_TO_BEGIN),
                    onContactClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Please press [search] to begin your search.").assertIsDisplayed()
    }

    @Test
    fun seekContactScreen_displaysNoUserFoundPrompt() {
        composeTestRule.setContent {
            NavigatorTheme {
                SeekContactScreen(
                    uiState = SeekContactUiState.Prompt(SeekPromptType.NO_USER_FOUND),
                    onContactClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("No user is found.").assertIsDisplayed()
    }

    @Test
    fun seekContactScreen_displaysContactResultAndInvokesClick() {
        var clickedContact: Contact? = null
        val contact = Contact(
            name = "Wiktor Nizio",
            email = "wiktor.nizio@gmail.com",
            color = android.graphics.Color.BLUE,
        )

        composeTestRule.setContent {
            NavigatorTheme {
                SeekContactScreen(
                    uiState = SeekContactUiState.ContactResult(contact),
                    onContactClick = { clickedContact = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Wiktor Nizio").assertIsDisplayed().performClick()
        assertTrue(clickedContact != null)
        assertEquals(contact, clickedContact)
    }

    @Test
    fun seekContactScreen_displaysConfirmationDialogAndInvokesConfirm() {
        var confirmed = false
        val contact = Contact(
            name = "Wiktor Nizio",
            email = "wiktor.nizio@gmail.com",
            color = android.graphics.Color.BLUE,
        )
        val targetContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.setContent {
            NavigatorTheme {
                SeekContactScreen(
                    uiState = SeekContactUiState.Hidden,
                    onContactClick = {},
                    confirmationContact = contact,
                    onConfirmAddContact = { confirmed = true },
                )
            }
        }

        val title = targetContext.getString(R.string.seek_contact_adding_friend_title)
        val yes = targetContext.getString(android.R.string.yes)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(yes).assertIsDisplayed().performClick()
        assertTrue(confirmed)
    }
}
