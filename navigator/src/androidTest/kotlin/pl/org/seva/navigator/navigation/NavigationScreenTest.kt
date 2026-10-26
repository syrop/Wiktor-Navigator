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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.main.ui.NavigatorTheme

class NavigationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun navigationScreen_displaysFabAndHandlesClick() {
        var addClicked = false

        composeTestRule.setContent {
            NavigatorTheme {
                NavigationScreen(
                    isStationary = false,
                    followingContact = null,
                    onAddContactClick = { addClicked = true },
                    onDismissFollowing = {},
                )
            }
        }

        val searchDesc = targetContext.getString(R.string.action_search)
        composeTestRule.onNodeWithContentDescription(searchDesc).assertIsDisplayed().performClick()
        assertTrue(addClicked)
    }

    @Test
    fun navigationScreen_displaysStationaryHud_whenStationary() {
        composeTestRule.setContent {
            NavigatorTheme {
                NavigationScreen(
                    isStationary = true,
                    followingContact = null,
                    onAddContactClick = {},
                    onDismissFollowing = {},
                )
            }
        }

        val stationaryText = targetContext.getString(R.string.device_not_moving)
        composeTestRule.onNodeWithText(stationaryText).assertIsDisplayed()
    }

    @Test
    fun navigationScreen_displaysFollowingContactHud() {
        val testContact = Contact(
            name = "Alice",
            email = "alice@example.com",
            color = android.graphics.Color.RED,
        )

        composeTestRule.setContent {
            NavigatorTheme {
                NavigationScreen(
                    isStationary = false,
                    followingContact = testContact,
                    onAddContactClick = {},
                    onDismissFollowing = {},
                )
            }
        }

        val followingText = targetContext.getString(R.string.navigation_following_name)
            .replace("[name]", "Alice")
        composeTestRule.onNodeWithText(followingText).assertIsDisplayed()
    }

    @Test
    fun navigationScreen_displaysHelpDialog_whenParamsProvided() {
        var buttonClicked = false

        composeTestRule.setContent {
            NavigatorTheme {
                NavigationScreen(
                    isStationary = false,
                    followingContact = null,
                    onAddContactClick = {},
                    onDismissFollowing = {},
                    helpDialogParams = HelpDialogParams(
                        htmlContent = "<html><body><h1>Help</h1></body></html>",
                        actionButtonText = "Grant Permission",
                        onActionButtonClick = { buttonClicked = true },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Grant Permission").assertIsDisplayed().performClick()
        assertTrue(buttonClicked)
    }
}
