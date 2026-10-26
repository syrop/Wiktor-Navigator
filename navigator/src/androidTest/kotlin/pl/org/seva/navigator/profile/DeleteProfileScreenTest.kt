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

package pl.org.seva.navigator.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.org.seva.navigator.main.ui.NavigatorTheme

class DeleteProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deleteProfileScreen_displaysDescriptionAndHandlesButtons() {
        var cancelClicked = false
        var okClicked = false

        composeTestRule.setContent {
            NavigatorTheme {
                DeleteProfileScreen(
                    onCancelClick = { cancelClicked = true },
                    onOkClick = { okClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        assertTrue(cancelClicked)

        composeTestRule.onNodeWithText("OK").assertIsDisplayed().performClick()
        assertTrue(okClicked)
    }
}
