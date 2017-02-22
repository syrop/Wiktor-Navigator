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

package pl.org.seva.navigator.model;

import android.support.annotation.NonNull;

public class Contact implements Comparable<Contact> {

    public String email;
    public String displayName;
    private String uid;

    public Contact setName(String name) {
        this.displayName = name;
        return this;
    }

    public Contact setEmail(String email) {
        this.email = email;
        return this;
    }

    public Contact setUid(String uid) {
        this.uid = uid;
        return this;
    }

    @Override
    public int compareTo(@NonNull Contact o) {
        return displayName.compareTo(o.displayName);
    }
}
