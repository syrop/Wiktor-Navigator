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

package pl.org.seva.navigator.presenter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.lang.ref.WeakReference;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.navigator.model.ContactsCache;
import pl.org.seva.navigator.model.database.firebase.FirebaseWriter;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.model.database.sqlite.SqliteWriter;
import pl.org.seva.navigator.view.builder.notification.PeerRequestedFriendshipNotificationBuilder;

@Singleton
public class FriendshipListener {

    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject
    ContactsCache contactsCache;
    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject
    SqliteWriter sqliteWriter;
    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject FirebaseWriter firebaseWriter;

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
        acceptedReceiver = new FriendshipAcceptedBroadcastReceiver();
        rejectedReceiver = new FriendshipRejectedBroadcastReceiver();
        context.registerReceiver(acceptedReceiver, new IntentFilter(FRIENDSHIP_ACCEPTED_INTENT));
        context.registerReceiver(rejectedReceiver, new IntentFilter(FRIENDSHIP_REJECTED_INTENT));
        int notificationId = new Random().nextInt();
        Intent friendshipAccepted = new Intent(FRIENDSHIP_ACCEPTED_INTENT)
                .putExtra(Contact.PARCELABLE_KEY, contact)
                .putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent yesPi = PendingIntent.getBroadcast(
                context,
                0,
                friendshipAccepted,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Intent friendshipRejected = new Intent(FRIENDSHIP_REJECTED_INTENT)
                .putExtra(Contact.PARCELABLE_KEY, contact)
                .putExtra(NOTIFICATION_ID, notificationId);
        PendingIntent noPi = PendingIntent.getBroadcast(
                context,
                0,
                friendshipRejected,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new PeerRequestedFriendshipNotificationBuilder(context)
                .setContact(contact)
                .setNoPendingIntent(noPi)
                .setYesPendingIntent(yesPi)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

    public void onPeerAcceptedFriendship(Contact contact) {
        contactsCache.add(contact);
        sqliteWriter.addFriend(contact);
    }

    public void onPeerDeletedFriendship(Contact contact) {
        contactsCache.delete(contact);
        sqliteWriter.deleteFriend(contact);
    }

    public void onFriendRead(Contact contact) {
        contactsCache.add(contact);
    }

    private class FriendshipAcceptedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            Contact contact = intent.getParcelableExtra(Contact.PARCELABLE_KEY);
            if (contactsCache.contains(contact)) {
                return;
            }
            contactsCache.add(contact);
            sqliteWriter.addFriend(contact);
            firebaseWriter.acceptFriendship(contact);
            context.unregisterReceiver(this);
            context.unregisterReceiver(rejectedReceiver);
        }
    }

    private class FriendshipRejectedBroadcastReceiver extends BroadcastReceiver {

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
