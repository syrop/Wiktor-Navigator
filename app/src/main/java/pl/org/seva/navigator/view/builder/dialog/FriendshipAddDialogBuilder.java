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
                .setTitle(R.string.search_dialog_title)
                .setMessage(context.getString(R.string.search_dialog_question).replace(NAME_TAG, contact.name()))
                .setPositiveButton(android.R.string.yes, ((dialog, which) -> yesAction.run()))
                .setNegativeButton(android.R.string.no, ((dialog, which) -> noAction.run()))
                .create();
    }
}
