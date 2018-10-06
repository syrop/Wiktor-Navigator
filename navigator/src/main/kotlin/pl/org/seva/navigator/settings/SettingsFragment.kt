/*
 * Copyright (C) 2018 Wiktor Nizio
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

package pl.org.seva.navigator.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import pl.org.seva.navigator.R
import pl.org.seva.navigator.debug.debug

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    private fun onDebugClicked(checkbox: CheckBoxPreference) {
        if (!checkbox.isChecked) {
            debug.stop()
            return
        }
        val builder = AlertDialog.Builder(activity!!, android.R.style.Theme_Material_Dialog_Alert)
        builder.setMessage(R.string.settings_fragment_debug_info)
                .setTitle(R.string.pref_debug_mode)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        debug.start()
    }

    override fun onPreferenceTreeClick(preference: Preference) = when (preference.key) {
            DEBUG -> {
                onDebugClicked(preference as CheckBoxPreference)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }

    companion object {
        const val DEBUG = "debug"
    }
}
