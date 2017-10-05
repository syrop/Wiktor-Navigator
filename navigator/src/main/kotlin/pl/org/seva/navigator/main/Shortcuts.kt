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

package pl.org.seva.navigator.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import pl.org.seva.navigator.R
import pl.org.seva.navigator.data.ContactsStore
import pl.org.seva.navigator.data.model.Contact
import pl.org.seva.navigator.ui.activity.NavigationActivity

@SuppressLint("NewApi")
fun setDynamicShortcuts(context: Context) {
    @Suppress("unused")
    fun Contact.shortcut() = ShortcutInfo.Builder(context, System.nanoTime().toString())
                .setShortLabel(name)
                .setIntent(Intent(Intent.ACTION_MAIN, Uri.EMPTY, context, NavigationActivity::class.java)
                        .putExtra(NavigationActivity.CONTACT_EMAIL_EXTRA, email))
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
        return
    }
    val cm = context.getSystemService(ShortcutManager::class.java)
    val shortcuts = Kodein.global.instance<ContactsStore>().snapshot()
            .asSequence()
            .take(cm.maxShortcutCountPerActivity)
            .map { it.shortcut() }
            .toList()
    cm.dynamicShortcuts = shortcuts
}
