package pl.org.seva.navigator.settings

import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import pl.org.seva.navigator.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            DEBUG -> true
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    companion object {
        const val DEBUG = "debug"
    }
}
