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

package pl.org.seva.navigator.presenter.listener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.NotificationCompat;

import java.lang.ref.WeakReference;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.presenter.database.sqlite.SqliteWriter;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.model.ContactsMemoryCache;

@Singleton
public class FriendshipListener {

    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject ContactsMemoryCache contactsMemoryCache;
    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject SqliteWriter sqliteWriter;

    private static final String FRIENDSHIP_ACCEPTED_INTENT = "friendship_accepted_intent";
    private static final String FRIENDSHIP_REJECTED_INTENT = "friendship_rejected_intent";
    private static final String NOTIFICATION_ID = "notification_id";
    private BroadcastReceiver acceptedReceiver;
    private BroadcastReceiver rejectedReceiver;

    private WeakReference<Context> weakContext;

    @Inject
    FriendshipListener() {
    }

    public void init(Context context) {
        this.weakContext = new WeakReference<>(context);
    }

    public void onPeerRequestedFriendship(Contact contact) {
        Context context = weakContext.get();
        if (context == null) {
            return;
        }
        acceptedReceiver = new FriendshipAccepted();
        rejectedReceiver = new FriendshipRejected();
        context.registerReceiver(acceptedReceiver, new IntentFilter(FRIENDSHIP_ACCEPTED_INTENT));
        context.registerReceiver(rejectedReceiver, new IntentFilter(FRIENDSHIP_REJECTED_INTENT));
        String message = context.getResources()
                .getString(R.string.friendship_confirmation)
                .replace("[name]", contact.name())
                .replace("[email]", contact.email());
        int notificationId = new Random().nextInt();
        Intent friendshipAcceptedIntent = new Intent(FRIENDSHIP_ACCEPTED_INTENT)
                .putExtra(Contact.PARCELABLE_NAME, contact)
                .putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent yesPi = PendingIntent.getBroadcast(
                context,
                0,
                friendshipAcceptedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Intent friendshipRejectedIntent = new Intent(FRIENDSHIP_REJECTED_INTENT)
                .putExtra(Contact.PARCELABLE_NAME, contact)
                .putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent noPi = PendingIntent.getActivity(
                context,
                0,
                friendshipRejectedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // http://stackoverflow.com/questions/6357450/android-multiline-notifications-notifications-with-longer-text#22964072
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(context.getString(R.string.app_name));
        bigTextStyle.bigText(message);

        // http://stackoverflow.com/questions/11883534/how-to-dismiss-notification-after-action-has-been-clicked#11884313
        Notification notification = new NotificationCompat.Builder(context)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_close_black_24dp, context.getString(android.R.string.no), noPi)
                .addAction(R.drawable.ic_check_black_24dp, context.getString(android.R.string.yes), yesPi)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

    public void onPeerAcceptedFriendship(Contact contact) {
        contactsMemoryCache.add(contact);
        sqliteWriter.persistFriend(contact);
    }

    public void onPeerDeletedFriendship(Contact contact) {
        contactsMemoryCache.delete(contact);
        sqliteWriter.deleteFriend(contact);
    }

    private class FriendshipAccepted extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            Contact contact = intent.getParcelableExtra(Contact.PARCELABLE_NAME);
            if (contactsMemoryCache.contains(contact)) {
                return;
            }
            contactsMemoryCache.add(contact);
            sqliteWriter.persistFriend(contact);
            context.unregisterReceiver(this);
            context.unregisterReceiver(rejectedReceiver);
        }
    }

    private class FriendshipRejected extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            context.unregisterReceiver(this);
            context.unregisterReceiver(acceptedReceiver);
        }
    }
}
