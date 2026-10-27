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

package pl.org.seva.navigator.main.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.org.seva.navigator.contact.Contact

@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private lateinit var db: NavigatorDbAbstract
    private lateinit var dao: ContactDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NavigatorDbAbstract::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.contactDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetAllContacts() {
        val contact1 = Contact(email = "alice@example.com", name = "Alice")
        val contact2 = Contact(email = "bob@example.com", name = "Bob")

        dao.insert(contact1.toEntity())
        dao.insert(contact2.toEntity())

        val results = dao.getAll().map { it.value() }
        assertEquals(2, results.size)
        assertTrue(results.any { it.email == "alice@example.com" && it.name == "Alice" })
        assertTrue(results.any { it.email == "bob@example.com" && it.name == "Bob" })
    }

    @Test
    fun deleteContact() {
        val contact = Contact(email = "charlie@example.com", name = "Charlie")
        dao.insert(contact.toEntity())
        assertEquals(1, dao.getAll().size)

        dao.delete(contact.toEntity())
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun deleteAllContacts() {
        dao.insert(Contact(email = "user1@example.com", name = "User 1").toEntity())
        dao.insert(Contact(email = "user2@example.com", name = "User 2").toEntity())
        assertEquals(2, dao.getAll().size)

        val deletedCount = dao.deleteAll()
        assertEquals(2, deletedCount)
        assertTrue(dao.getAll().isEmpty())
    }
}
