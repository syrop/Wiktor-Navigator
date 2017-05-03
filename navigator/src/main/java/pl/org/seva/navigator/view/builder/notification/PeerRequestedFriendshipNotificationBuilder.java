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
package pl.org.seva.navigator.view.builder.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v7.app.NotificationCompat;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.model.Contact;

public final class PeerRequestedFriendshipNotificationBuilder {

    private static final String NAME_TAG = "[name]";
    private static final String EMAIL_TAG = "[email]";

    private final Context context;
    private PendingIntent yesPi;
    private PendingIntent noPi;
    private Contact contact;

    public PeerRequestedFriendshipNotificationBuilder(Context context) {
        this.context = context;
    }

    public PeerRequestedFriendshipNotificationBuilder setYesPendingIntent(PendingIntent yesPi) {
        this.yesPi = yesPi;
        return this;
    }

    public PeerRequestedFriendshipNotificationBuilder setContact(Contact contact) {
        this.contact = contact;
        return this;
    }

    public PeerRequestedFriendshipNotificationBuilder setNoPendingIntent(PendingIntent noPi) {
        this.noPi = noPi;
        return this;
    }

    public Notification build() {
        String message = context.getResources()
                .getString(R.string.friendship_confirmation)
                .replace(NAME_TAG, contact.name())
                .replace(EMAIL_TAG, contact.email());

        // http://stackoverflow.com/questions/6357450/android-multiline-notifications-notifications-with-longer-text#22964072
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(context.getString(R.string.app_name));
        bigTextStyle.bigText(message);

        // http://stackoverflow.com/questions/11883534/how-to-dismiss-notification-after-action-has-been-clicked#11884313
        return new NotificationCompat.Builder(context)
                .setStyle(bigTextStyle)
                .setContentText(context.getText(R.string.friendship_requested_notification_short))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_close_black_24dp, context.getString(android.R.string.no), noPi)
                .addAction(R.drawable.ic_check_black_24dp, context.getString(android.R.string.yes), yesPi)
                .build();
    }
}
