package pl.org.seva.navigator.settings

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import pl.org.seva.navigator.R
import pl.org.seva.navigator.debug.debug

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    private fun onDebugClicked(checkbox: CheckBoxPreference) {
        if (!checkbox.isChecked) {
            debug().stop()
            return
        }
        val builder = AlertDialog.Builder(activity!!, android.R.style.Theme_Material_Dialog_Alert)
        builder.setMessage(R.string.settings_activity_debug_info)
                .setTitle(R.string.pref_debug_mode)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        debug().start()
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
