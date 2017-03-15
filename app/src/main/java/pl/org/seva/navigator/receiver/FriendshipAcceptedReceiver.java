package pl.org.seva.navigator.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import pl.org.seva.navigator.manager.ContactManager;
import pl.org.seva.navigator.manager.DatabaseManager;
import pl.org.seva.navigator.model.Contact;

public class FriendshipAcceptedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Contact contact = intent.getParcelableExtra(Contact.PARCELABLE_NAME);
        ContactManager.getInstance().add(contact);
        DatabaseManager.getInstance().persistFriend(contact);
    }
}
