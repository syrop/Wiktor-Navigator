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

package pl.org.seva.navigator.shortcut

import android.content.Context
import android.content.pm.ShortcutInfo
import android.os.Build
import android.support.annotation.RequiresApi
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import pl.org.seva.navigator.data.ContactsStore
import pl.org.seva.navigator.data.model.Contact

fun setDynamicShortcuts(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
        return
    }
    val shortcuts = Kodein.global.instance<ContactsStore>().snapshot()
            .asSequence()
            .map { it.shortcut(context) }
            .toList()
}

@RequiresApi(Build.VERSION_CODES.N_MR1)
fun Contact.shortcut(context: Context) =
        FriendShortcutBuilder(context).apply {
            label = name
        }.build()

class FriendShortcutBuilder(private val context: Context) {
    lateinit var label: String

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun build(): ShortcutInfo = ShortcutInfo.Builder(context, ID)
            .setShortLabel(label)
            .build()

    companion object {
        val ID = "id1"
    }
}
