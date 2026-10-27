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

package pl.org.seva.navigator.contact

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactTest {

    @Test
    fun contactsWithSameEmailAreEqual() {
        val contact1 = Contact(email = "test@example.com", name = "Test User", color = Color.RED)
        val contact2 = Contact(email = "test@example.com", name = "Different Name", color = Color.BLUE)

        assertEquals(contact1, contact2)
        assertEquals(contact1.hashCode(), contact2.hashCode())
    }

    @Test
    fun contactsWithDifferentEmailAreNotEqual() {
        val contact1 = Contact(email = "test1@example.com", name = "Test User")
        val contact2 = Contact(email = "test2@example.com", name = "Test User")

        assertNotEquals(contact1, contact2)
    }

    @Test
    fun compareTo_sortsByNameThenEmail() {
        val alice = Contact(email = "alice@example.com", name = "Alice")
        val bob = Contact(email = "bob@example.com", name = "Bob")
        val bobZ = Contact(email = "z_bob@example.com", name = "Bob")

        assertTrue(alice < bob)
        assertTrue(bob < bobZ)

        val list = listOf(bobZ, alice, bob)
        val sorted = list.sorted()
        assertEquals(listOf(alice, bob, bobZ), sorted)
    }

    @Test
    fun isEmpty_returnsTrueWhenEmailIsEmpty() {
        val emptyContact = Contact(email = "", name = "Nameless")
        assertTrue(emptyContact.isEmpty)

        val validContact = Contact(email = "user@example.com", name = "User")
        assertFalse(validContact.isEmpty)
    }

    @Test
    fun entityRoundTripPreservesProperties() {
        val original = Contact(
            email = "user@domain.com",
            name = "Display Name",
            color = Color.GREEN,
        )

        val entity = original.toEntity()
        assertEquals(original.email, entity.email)
        assertEquals(original.name, entity.name)
        assertEquals(original.color, entity.color)

        val reconstructed = entity.value()
        assertEquals(original.email, reconstructed.email)
        assertEquals(original.name, reconstructed.name)
        assertEquals(original.color, reconstructed.color)
    }
}
