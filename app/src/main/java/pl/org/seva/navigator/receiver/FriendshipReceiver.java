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

package pl.org.seva.navigator.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.database.sqlite.SqliteWriter;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.model.ContactsMemoryCache;

@Singleton
public class FriendshipReceiver {

    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject ContactsMemoryCache contactsMemoryCache;
    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject
    SqliteWriter sqliteWriter;

    private WeakReference<Context> weakContext;

    @Inject FriendshipReceiver() {
    }

    public void init(Context context) {
        this.weakContext = new WeakReference<>(context);
    }

    public void onPeerRequestedFriendship(Contact contact) {
        Context context = weakContext.get();
        if (context == null) {
            return;
        }
        String message = context.getResources()
                .getString(R.string.friendship_confirmation)
                .replace("[name]", contact.name())
                .replace("[email]", contact.email());
        PendingIntent noPi = PendingIntent.getActivity(
                context,
                0,
                new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Intent friendshipAcceptedIntent = new Intent(context, FriendshipAccepted.class)
                .putExtra(Contact.PARCELABLE_NAME, contact);
        PendingIntent yesPi = PendingIntent.getBroadcast(
                context,
                0,
                friendshipAcceptedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent contentPi = PendingIntent.getActivity(
                context,
                0,
                new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // http://stackoverflow.com/questions/6357450/android-multiline-notifications-notifications-with-longer-text#22964072
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(context.getString(R.string.app_name));
        bigTextStyle.bigText(message);

        // http://stackoverflow.com/questions/11883534/how-to-dismiss-notification-after-action-has-been-clicked#11884313
        Notification notification = new NotificationCompat.Builder(context)
                .setStyle(bigTextStyle)
                .setContentIntent(contentPi)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_close_black_24dp, context.getString(android.R.string.no), noPi)
                .addAction(R.drawable.ic_check_black_24dp, context.getString(android.R.string.yes), yesPi)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    public void onPeerAcceptedFriendship(Contact contact) {
        contactsMemoryCache.add(contact);
        sqliteWriter.persistFriend(contact);
    }

    public void onPeerDeletedFriendship(Contact contact) {
        contactsMemoryCache.delete(contact);
        sqliteWriter.deleteFriend(contact);
    }

    public static class FriendshipAccepted extends BroadcastReceiver {

        @SuppressWarnings("CanBeFinal")
        @Inject ContactsMemoryCache contactsMemoryCache;
        @SuppressWarnings("CanBeFinal")
        @Inject
        SqliteWriter sqliteWriter;

        @Override
        public void onReceive(Context context, Intent intent) {
            ((NavigatorApplication) context.getApplicationContext()).getGraph().inject(this);
            Contact contact = intent.getParcelableExtra(Contact.PARCELABLE_NAME);
            contactsMemoryCache.add(contact);
            sqliteWriter.persistFriend(contact);
        }
    }
}
