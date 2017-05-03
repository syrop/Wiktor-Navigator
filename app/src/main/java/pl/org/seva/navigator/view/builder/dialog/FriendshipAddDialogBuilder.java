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

package pl.org.seva.navigator.view.builder.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.app.AlertDialog;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.model.Contact;

public class FriendshipAddDialogBuilder {

    private static final String NAME_TAG = "[name]";

    private final Context context;
    private Contact contact;
    private Runnable yesAction;
    private Runnable noAction;

    public FriendshipAddDialogBuilder(Context context) {
        this.context = context;
    }

    public FriendshipAddDialogBuilder setContact(Contact contact) {
        this.contact = contact;
        return this;
    }

    public FriendshipAddDialogBuilder setYesAction(Runnable yesAction) {
        this.yesAction = yesAction;
        return this;
    }

    public FriendshipAddDialogBuilder setNoAction(Runnable noAction) {
        this.noAction = noAction;
        return this;
    }

    public Dialog build() {
        return new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.adding_friend_title)
                .setMessage(context.getString(R.string.add_friend_confirmation).replace(NAME_TAG, contact.name()))
                .setPositiveButton(android.R.string.yes, ((dialog, which) -> yesAction.run()))
                .setNegativeButton(android.R.string.no, ((dialog, which) -> noAction.run()))
                .create();
    }
}
