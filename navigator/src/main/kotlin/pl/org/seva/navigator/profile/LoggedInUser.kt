/*
 * Copyright (C) 2017 Wiktor Nizio
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

import com.google.firebase.auth.FirebaseUser
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.main.init.instance

val loggedInUser by instance<LoggedInUser>()

fun FirebaseUser.setCurrent() = loggedInUser setCurrentUser this

val isLoggedIn get() = loggedInUser.isLoggedIn

class LoggedInUser {

    val isLoggedIn get() = name != null && email != null
    var email: String? = null
    private var name: String? = null

    val loggedInContact get() = Contact(checkNotNull(email), checkNotNull(name))

    infix fun setCurrentUser(user: FirebaseUser?) {
        if (user != null) {
            email = user.email
            name = user.displayName
        }
        else {
            email = null
            name = null
        }
    }
}
