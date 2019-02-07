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

package pl.org.seva.navigator.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.*
import android.webkit.WebView
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController

import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_navigation.*
import org.apache.commons.io.IOUtils

import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.*
import pl.org.seva.navigator.main.fb.fbWriter
import pl.org.seva.navigator.main.*
import pl.org.seva.navigator.main.db.contactsDatabase
import pl.org.seva.navigator.profile.*

class NavigationFragment : Fragment() {

    private var isLocationPermissionGranted = false

    private var dialog: Dialog? = null
    private var snackbar: Snackbar? = null

    private lateinit var mapHolder: MapHolder

    private lateinit var navigatorModel: NavigatorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_navigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navigatorModel = ViewModelProviders.of(activity!!).get(NavigatorViewModel::class.java)
        mapHolder = createMapHolder {
            init(savedInstanceState, root, navigatorModel.contact.value)
            checkLocationPermission = this@NavigationFragment::ifLocationPermissionGranted
            persistCameraPositionAndZoom = this@NavigationFragment::persistCameraPositionAndZoom
        }
        add_contact_fab.setOnClickListener { onAddContactClicked() }

        checkLocationPermission()
        activityRecognition.listen(lifecycle) { state ->
            when (state) {
                ActivityRecognitionSource.STATIONARY -> hud_stationary.visibility = View.VISIBLE
                ActivityRecognitionSource.MOVING -> hud_stationary.visibility = View.GONE
            }
        }
        navigatorModel.contact.observe(this) { contact ->
            mapHolder.contact = contact
            contact.persist()
        }
        navigatorModel.deleteProfile.observe(this) { result ->
            if (result) {
                deleteProfile()
                navigatorModel.deleteProfile.value = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        peerObservable.clearPeerListeners()
    }

    override fun onResume() {
        super.onResume()
        checkLocationPermission(
                onGranted = {
                    snackbar?.dismiss()
                    if (!isLoggedIn) {
                        showLoginSnackbar()
                    }
                },
                onDenied = {})
        activity!!.invalidateOptionsMenu()
    }

    private fun onAddContactClicked() {
        mapHolder.stopWatchingPeer()
        if (!isLocationPermissionGranted) {
            checkLocationPermission()
        }
        else if (isLoggedIn) {
            findNavController().navigate(R.id.action_navigationFragment_to_contactsFragment)
        }
        else {
            showLoginSnackbar()
        }
    }

    private inline fun ifLocationPermissionGranted(f: () -> Unit) =
            checkLocationPermission(onGranted = f, onDenied = {})

    private inline fun checkLocationPermission(
            onGranted: () -> Unit = ::onLocationPermissionGranted,
            onDenied: () -> Unit = ::requestLocationPermission) =
                if (ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        isLocationPermissionGranted = true
                        onGranted.invoke()
                }
                else { onDenied.invoke() }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) =
        menuInflater.inflate(R.menu.navigation, menu)

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_help).isVisible =
                !isLocationPermissionGranted || !isLoggedIn
        menu.findItem(R.id.action_logout).isVisible = isLoggedIn
        menu.findItem(R.id.action_delete_user).isVisible = isLoggedIn
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        fun help(caption: Int, file: String, action: () -> Unit): Boolean {
                dialog = Dialog(context!!).apply {
                setContentView(R.layout.dialog_help)
                val web = findViewById<WebView>(R.id.web)
                web.settings.defaultTextEncodingName = UTF_8
                findViewById<Button>(R.id.action_button).setText(caption)
                val content = IOUtils.toString(activity!!.assets.open(file), UTF_8)
                        .replace(APP_VERSION_PLACEHOLDER, versionName)
                        .replace(APP_NAME_PLACEHOLDER, getString(R.string.app_name))
                web.loadDataWithBaseURL(ASSET_DIR, content, PLAIN_TEXT, UTF_8, null)

                findViewById<View>(R.id.action_button).setOnClickListener {
                    action()
                    dismiss()
                }
                show()
            }
            return true
        }

        fun showLocationPermissionHelp() = help(
                R.string.dialog_settings_button,
                HELP_LOCATION_PERMISSION_EN,
                action = ::onSettingsClicked)

        fun showLoginHelp() = help(R.string.dialog_login_button, HELP_LOGIN_EN, action = ::login)

        return when (item.itemId) {
            R.id.action_logout -> logout()
            R.id.action_delete_user -> {
                findNavController().navigate(R.id.action_navigationFragment_to_deleteProfileFragment)
                true
            }
            R.id.action_help -> if (!isLocationPermissionGranted) {
                showLocationPermissionHelp()
            }
            else if (!isLoggedIn) {
                showLoginHelp()
            } else true
            R.id.action_settings -> {
                findNavController().navigate(R.id.action_navigationFragment_to_settingsFragmentContainer)
                true
            }
            R.id.action_credits -> {
                findNavController().navigate(R.id.action_navigationFragment_to_creditsFragment)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onSettingsClicked() {
        dialog?.dismiss()
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", activity!!.packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun requestLocationPermission() {
        fun showLocationPermissionSnackbar() {
            snackbar = Snackbar.make(
                    coordinator,
                    R.string.snackbar_permission_request_denied,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_retry)  { requestLocationPermission() }
                    .apply { show() }
        }

        requestPermissions(
                Permissions.DEFAULT_PERMISSION_REQUEST_ID,
                arrayOf(Permissions.PermissionRequest(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        onGranted = ::onLocationPermissionGranted,
                        onDenied = ::showLocationPermissionSnackbar)))
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        activity!!.invalidateOptionsMenu()
        mapHolder.locationPermissionGranted()
        if (isLoggedIn) {
            (activity!!.application as NavigatorApplication).startService()
        }
    }

    private fun showLoginSnackbar() {
        snackbar = Snackbar.make(
                coordinator,
                R.string.snackbar_please_log_in,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_login) { login() }
                .apply { show() }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            requests: Array<String>,
            grantResults: IntArray) =
            permissions.onRequestPermissionsResult(requestCode, requests, grantResults)

    private fun login() {
        dialog?.dismiss()
        activity!!.loginActivity(LoginActivity.LOGIN)
    }

    private fun logout(): Boolean {
        null.persist()
        mapHolder.stopWatchingPeer()
        activity!!.loginActivity(LoginActivity.LOGOUT)
        return true
    }

    private fun deleteProfile() {
        mapHolder.stopWatchingPeer()
        contacts.clear()
        contactsDatabase.contactDao.deleteAll()
        setDynamicShortcuts(context!!)
        fbWriter.deleteMe()
        logout()
    }

    @SuppressLint("CommitPrefEdits")
    private fun persistCameraPositionAndZoom() =
            with (PreferenceManager.getDefaultSharedPreferences(context).edit()) {
                putFloat(ZOOM_PROPERTY, mapHolder.zoom)
                putFloat(LATITUDE_PROPERTY, mapHolder.lastCameraPosition.latitude.toFloat())
                putFloat(LONGITUDE_PROPERTY, mapHolder.lastCameraPosition.longitude.toFloat())
                apply()
            }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(SAVED_PEER_LOCATION, mapHolder.peerLocation)
        super.onSaveInstanceState(outState)
    }

    companion object {

        private const val UTF_8 = "UTF-8"
        private const val ASSET_DIR = "file:///android_asset/"
        private const val PLAIN_TEXT = "text/html"
        private const val APP_VERSION_PLACEHOLDER = "[app_version]"
        private const val APP_NAME_PLACEHOLDER = "[app_name]"
        private const val HELP_LOCATION_PERMISSION_EN = "help_location_permission_en.html"
        private const val HELP_LOGIN_EN = "help_login_en.html"

        const val SAVED_PEER_LOCATION = "saved_peer_location"

        const val ZOOM_PROPERTY = "navigation_map_zoom"
        const val LATITUDE_PROPERTY = "navigation_map_latitude"
        const val LONGITUDE_PROPERTY = "navigation_map_longitude"
        const val DEFAULT_ZOOM = 7.5f
    }
}
