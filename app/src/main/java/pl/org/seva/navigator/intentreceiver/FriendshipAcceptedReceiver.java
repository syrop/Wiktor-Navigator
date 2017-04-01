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
 */

package pl.org.seva.navigator.intentreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

import pl.org.seva.navigator.database.SqliteDataBaseManager;
import pl.org.seva.navigator.model.ContactsMemoryCache;
import pl.org.seva.navigator.model.Contact;

public class FriendshipAcceptedReceiver extends BroadcastReceiver {

    @Inject
    ContactsMemoryCache contactsMemoryCache;
    @Inject
    SqliteDataBaseManager sqliteDataBaseManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Contact contact = intent.getParcelableExtra(Contact.PARCELABLE_NAME);
        contactsMemoryCache.add(contact);
        sqliteDataBaseManager.persistFriend(contact);
    }
}
