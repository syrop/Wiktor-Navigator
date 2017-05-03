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
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.model.Contact;

public class FriendshipDeleteDialogBuilder {

    private static final String NAME_PLACEHOLDER = "[name]";

    private final Context context;
    private Contact contact;
    private Runnable onConfirmedAction;

    public FriendshipDeleteDialogBuilder(Context context) {
        this.context = context;
    }

    public FriendshipDeleteDialogBuilder setContact(Contact contact) {
        this.contact = contact;
        return this;
    }

    public FriendshipDeleteDialogBuilder setOnConfirmedAction(Runnable onConfirmedAction) {
        this.onConfirmedAction = onConfirmedAction;
        return this;
    }

    public Dialog build() {
        String message = context.getString(R.string.delete_friend_confirmation)
                .replace(NAME_PLACEHOLDER, contact.name());
        return new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.deleting_friend_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, ((dialog, which) -> onConfirmed(dialog)))
                .setNegativeButton(android.R.string.no, ((dialog, which) -> dialog.dismiss()))
                .create();
    }

    private void onConfirmed(DialogInterface dialog) {
        dialog.dismiss();
        onConfirmedAction.run();
    }
}
