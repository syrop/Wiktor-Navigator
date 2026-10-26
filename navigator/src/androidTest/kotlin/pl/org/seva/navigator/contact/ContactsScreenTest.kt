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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.org.seva.navigator.main.ui.NavigatorTheme

class ContactsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun contactsScreen_displaysEmptyPromptWhenNoContacts() {
        var addClicked = false

        composeTestRule.setContent {
            NavigatorTheme {
                ContactsScreen(
                    contactsList = emptyList(),
                    onContactClick = {},
                    onContactSwipedToDelete = {},
                    onAddContactClick = { addClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Please press + to search for contacts.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed().performClick()
        assertTrue(addClicked)
    }

    @Test
    fun contactsScreen_displaysContactsAndHandlesSelection() {
        var selectedContact: Contact? = null
        val contact1 = Contact(
            name = "Wiktor Nizio",
            email = "wiktor.nizio@gmail.com",
            color = android.graphics.Color.BLUE,
        )
        val contact2 = Contact(
            name = "Alice",
            email = "alice@example.com",
            color = android.graphics.Color.MAGENTA,
        )

        composeTestRule.setContent {
            NavigatorTheme {
                ContactsScreen(
                    contactsList = listOf(contact1, contact2),
                    onContactClick = { selectedContact = it },
                    onContactSwipedToDelete = {},
                    onAddContactClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Wiktor Nizio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()

        composeTestRule.onNodeWithText("Alice").performClick()
        assertEquals(contact2, selectedContact)
    }
}
