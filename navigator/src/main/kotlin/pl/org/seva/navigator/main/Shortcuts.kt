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
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.navigator.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.contact.contacts
import pl.org.seva.navigator.navigation.NavigationFragment

@SuppressLint("NewApi")
fun setShortcuts() {
    @Suppress("unused")
    fun Contact.shortcut() = ShortcutInfo.Builder(appContext, System.nanoTime().toString())
                .setShortLabel(name)
                .setIntent(Intent(Intent.ACTION_MAIN, Uri.EMPTY, appContext, NavigationFragment::class.java)
                        .putExtra(NavigatorActivity.CONTACT_EMAIL_EXTRA, email))
                .setIcon(Icon.createWithResource(appContext, R.mipmap.ic_launcher))
                .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
        return
    }
    val shortcutManager = appContext.getSystemService(ShortcutManager::class.java)
    val shortcuts = contacts.snapshot()
            .asSequence()
            .take(shortcutManager.maxShortcutCountPerActivity)
            .map { it.shortcut() }
            .toList()
    shortcutManager.dynamicShortcuts = shortcuts
}
