package pl.org.seva.navigator.view.builder.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.model.Contact;

public class FriendshipDeleteDialogBuilder {

    private static final String NAME_TAG = "[name]";

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
        String message = context.getString(R.string.search_dialog_question).replace(NAME_TAG, contact.name());
        return new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.search_dialog_title)
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
