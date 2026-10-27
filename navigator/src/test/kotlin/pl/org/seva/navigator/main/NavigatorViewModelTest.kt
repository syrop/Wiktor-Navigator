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
 */

package pl.org.seva.navigator.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.org.seva.navigator.contact.Contact

class NavigatorViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun initialLiveDataValuesAreNull() {
        val viewModel = NavigatorViewModel()
        assertNull(viewModel.contact.value)
        assertNull(viewModel.deleteProfile.value)
        assertNull(viewModel.query.value)
    }

    @Test
    fun updatesContactLiveData() {
        val viewModel = NavigatorViewModel()
        val contact = Contact(email = "test@example.com", name = "Test")
        viewModel.contact.value = contact
        assertEquals(contact, viewModel.contact.value)
    }

    @Test
    fun updatesQueryLiveData() {
        val viewModel = NavigatorViewModel()
        viewModel.query.value = "search query"
        assertEquals("search query", viewModel.query.value)
    }

    @Test
    fun updatesDeleteProfileLiveData() {
        val viewModel = NavigatorViewModel()
        viewModel.deleteProfile.value = true
        assertTrue(viewModel.deleteProfile.value == true)
    }
}
