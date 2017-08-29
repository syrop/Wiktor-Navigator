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

package pl.org.seva.navigator.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance

import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import io.reactivex.disposables.Disposables
import kotlinx.android.synthetic.main.activity_navigation.*
import org.apache.commons.io.IOUtils

import pl.org.seva.navigator.R
import pl.org.seva.navigator.application.NavigatorApplication
import pl.org.seva.navigator.data.Contact
import pl.org.seva.navigator.data.ContactsStore
import pl.org.seva.navigator.data.Login
import pl.org.seva.navigator.data.firebase.FbWriter
import pl.org.seva.navigator.data.room.ContactsDatabase
import pl.org.seva.navigator.listener.Permissions
import pl.org.seva.navigator.source.PeerLocationSource
import pl.org.seva.navigator.view.activity.viewholder.NavigationViewHolder
import pl.org.seva.navigator.view.activity.viewholder.navigationView
import pl.org.seva.navigator.view.googlemap.mapFragment
import pl.org.seva.navigator.view.googlemap.ready

class NavigationActivity : AppCompatActivity(), KodeinGlobalAware {

    private val peerLocationSource: PeerLocationSource = instance()
    private val store: ContactsStore = instance()
    private val permissions: Permissions = instance()
    private val login: Login = instance()
    private val fbWriter: FbWriter = instance()

    private var backClickTime = 0L

    private var mapFragment: SupportMapFragment? = null

    private var contact: Contact? = null
        set(value) {
            field = value
            value.persist()
        }
    private var permissionDisposable = Disposables.empty()
    private var isLocationPermissionGranted = false

    private var dialog: Dialog? = null
    private var snackbar: Snackbar? = null

    private var mapContainerId: Int = 0

    private var exitApplicationToast: Toast? = null

    private lateinit var viewHolder: NavigationViewHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readContactFromProperties()
        viewHolder = navigationView(this) { init(savedInstanceState) }
        setContentView(R.layout.activity_navigation)

        supportActionBar?.title = getString(R.string.navigation_activity_label)

        mapContainerId = map_container.id
        fab.setOnClickListener { onFabClicked() }
        checkLocationPermission()
    }

    private fun NavigationViewHolder.init(savedInstanceState: Bundle?) {
        val properties = PreferenceManager.getDefaultSharedPreferences(this@NavigationActivity)
        zoom = properties.getFloat(ZOOM_PROPERTY, DEFAULT_ZOOM)
        lastCameraPosition = LatLng(properties.getFloat(LATITUDE_PROPERTY, 0.0f).toDouble(),
                properties.getFloat(LONGITUDE_PROPERTY, 0.0f).toDouble())
        contact = this@NavigationActivity.contact
        checkLocationPermission = this@NavigationActivity::ifLocationPermissionGranted
        persistCameraPositionAndZoom = this@NavigationActivity::persistCameraPositionAndZoom
        if (savedInstanceState != null) {
            animateCamera = false
            peerLocation = savedInstanceState.getParcelable<LatLng?>(SAVED_PEER_LOCATION)
        }
        view = root
    }

    override fun onDestroy() {
        super.onDestroy()
        peerLocationSource.clearPeerLocationListeners()
    }

    override fun onResume() {
        super.onResume()
        checkLocationPermission(
                onGranted = {
                    snackbar?.dismiss()
                    if (!login.isLoggedIn) {
                        showLoginSnackbar()
                    }
                },
                onDenied = {})
        invalidateOptionsMenu()
        mapFragment = mapFragment {
            fm = supportFragmentManager
            container = mapContainerId
            tag = MAP_FRAGMENT_TAG
        } ready {
            viewHolder ready this
        }
    }

    private fun Contact?.persist() {
        val name = this?.name ?: ""
        val email = this?.email ?: ""
        PreferenceManager.getDefaultSharedPreferences(this@NavigationActivity).edit()
                .putString(CONTACT_NAME_PROPERTY, name)
                .putString(CONTACT_EMAIL_PROPERTY, email).apply()
    }

    private fun readContactFromProperties() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val name = preferences.getString(CONTACT_NAME_PROPERTY, "")
        val email = preferences.getString(CONTACT_EMAIL_PROPERTY, "")
        if (name.isNotEmpty() && email.isNotEmpty()) {
            val contact = Contact(email = email, name = name)
            if (store.contains(contact)) {
                this.contact = contact
            }
        }
    }

    private fun onFabClicked() {
        viewHolder.stopWatchingPeer()
        if (!isLocationPermissionGranted) {
            checkLocationPermission()
        } else if (login.isLoggedIn) {
            startActivityForResult(
                    Intent(this, ContactsActivity::class.java),
                    CONTACTS_ACTIVITY_REQUEST_ID)
        } else {
            showLoginSnackbar()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CONTACTS_ACTIVITY_REQUEST_ID -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewHolder.contact = data?.getParcelableExtra(CONTACT_IN_INTENT)
                }
                viewHolder.updateHud()
            }
            DELETE_PROFILE_REQUEST_ID -> {
                if (resultCode == Activity.RESULT_OK) {
                    deleteProfile()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    inline private fun ifLocationPermissionGranted(f: () -> Unit) =
            checkLocationPermission(onGranted = f, onDenied = {})

    inline private fun checkLocationPermission(
            onGranted: () -> Unit = this::onLocationPermissionGranted,
            onDenied: () -> Unit = this::requestLocationPermission) = if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true
                onGranted.invoke()
            } else {
                onDenied.invoke()
            }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.navigation, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_help).isVisible = !isLocationPermissionGranted || !login.isLoggedIn
        menu.findItem(R.id.action_logout).isVisible = login.isLoggedIn
        menu.findItem(R.id.action_delete_user).isVisible = login.isLoggedIn
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_logout -> {
            logout()
            true
        }
        R.id.action_delete_user -> {
            onDeleteProfileClicked()
            true
        }
        R.id.action_help -> {
            if (!isLocationPermissionGranted) {
                showLocationPermissionHelp()
            } else if (!login.isLoggedIn) {
                showLoginHelp()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showLocationPermissionHelp() = help(
            R.layout.dialog_help_location_permission,
            HELP_LOCATION_PERMISSION_EN,
            action = this::onSettingsClicked)

    private fun showLoginHelp() = help(R.layout.dialog_help_login, HELP_LOGIN_EN, action = this::login)

    private fun help(layout: Int, file: String, action: () -> Unit) {
        dialog = Dialog(this)
        dialog!!.setContentView(layout)
        val web = dialog!!.findViewById<WebView>(R.id.web)
        web.settings.defaultTextEncodingName = UTF_8

        val content = IOUtils.toString(assets.open(file), UTF_8)
                .replace(APP_VERSION_PLACEHOLDER, versionName)
                .replace(APP_NAME_PLACEHOLDER, getString(R.string.app_name))
        web.loadDataWithBaseURL(ASSET_DIR, content, PLAIN_TEXT, UTF_8, null)

        dialog!!.findViewById<View>(R.id.action_button).setOnClickListener { action() }
        dialog!!.show()
    }

    private val versionName: String
        get() = packageManager.getPackageInfo(packageName, 0).versionName

    private fun onSettingsClicked() {
        dialog?.dismiss()
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun requestLocationPermission() {
        permissionDisposable = permissions.request(
                this,
                Permissions.LOCATION_PERMISSION_REQUEST_ID,
                arrayOf(Permissions.PermissionRequest(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        onGranted = this::onLocationPermissionGranted,
                        onDenied = this::onLocationPermissionDenied)))
    }

    override fun onStop() {
        permissionDisposable.dispose()
        super.onStop()
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        permissionDisposable.dispose()
        invalidateOptionsMenu()
        viewHolder.locationPermissionGranted()
        if (!login.isLoggedIn) {
            showLoginSnackbar()
        } else {
            (application as NavigatorApplication).startService()
        }
    }

    private fun onLocationPermissionDenied() {
        permissionDisposable.dispose()
        showLocationPermissionSnackbar()
    }

    private fun showLocationPermissionSnackbar() {
        snackbar = Snackbar.make(
                map_container,
                R.string.snackbar_permission_request_denied,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_retry)  { requestLocationPermission() }
        snackbar!!.show()
    }

    private fun showLoginSnackbar() {
        snackbar = Snackbar.make(
                map_container,
                R.string.snackbar_please_log_in,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_login) { login() }
        snackbar!!.show()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) =
            this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)

    private fun login() {
        dialog?.dismiss()
        startActivity(Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGIN))
    }

    private fun logout() {
        viewHolder.stopWatchingPeer()
        startActivity(Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGOUT))
    }

    private fun deleteProfile() {
        viewHolder.stopWatchingPeer()
        store.clear()
        instance<ContactsDatabase>().contactDao.deleteAll()
        fbWriter.deleteMe()
        logout()
    }

    private fun onDeleteProfileClicked() {
        val intent = Intent(this, DeleteProfileActivity::class.java)
        startActivityForResult(intent, DELETE_PROFILE_REQUEST_ID)
    }

    @SuppressLint("CommitPrefEdits")
    private fun persistCameraPositionAndZoom() =
            with (PreferenceManager.getDefaultSharedPreferences(this).edit()) {
                putFloat(ZOOM_PROPERTY, viewHolder.zoom)
                putFloat(LATITUDE_PROPERTY, viewHolder.lastCameraPosition.latitude.toFloat())
                putFloat(LONGITUDE_PROPERTY, viewHolder.lastCameraPosition.longitude.toFloat())
                apply()
            }

    override fun onSaveInstanceState(outState: Bundle) {
        deleteMapFragment()
        outState.putParcelable(SAVED_PEER_LOCATION, viewHolder.peerLocation)
        super.onSaveInstanceState(outState)
    }

    private fun deleteMapFragment() {
        mapFragment?.apply {
            fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            mapFragment = null
        }
    }

    override fun onBackPressed() = if (System.currentTimeMillis() - backClickTime < DOUBLE_CLICK_MS) {
        (application as NavigatorApplication).stopService()
        exitApplicationToast?.cancel()
        super.onBackPressed()
    } else {
        exitApplicationToast?.cancel()
        exitApplicationToast =
                Toast.makeText(this, R.string.tap_back_second_time, Toast.LENGTH_SHORT)
        exitApplicationToast!!.show()
        backClickTime = System.currentTimeMillis()
    }

    companion object {

        private val DELETE_PROFILE_REQUEST_ID = 0
        private val CONTACTS_ACTIVITY_REQUEST_ID = 1

        private val UTF_8 = "UTF-8"
        private val ASSET_DIR = "file:///android_asset/"
        private val PLAIN_TEXT = "text/html"
        private val APP_VERSION_PLACEHOLDER = "[app_version]"
        private val APP_NAME_PLACEHOLDER = "[app_name]"
        private val HELP_LOCATION_PERMISSION_EN = "help_location_permission_en.html"
        private val HELP_LOGIN_EN = "help_login_en.html"

        val CONTACT_IN_INTENT = "contact"

        private val MAP_FRAGMENT_TAG = "map"

        private val SAVED_PEER_LOCATION = "saved_peer_location"

        private val ZOOM_PROPERTY = "navigation_map_zoom"
        private val LATITUDE_PROPERTY = "navigation_map_latitude"
        private val LONGITUDE_PROPERTY = "navigation_map_longitude"
        private val CONTACT_NAME_PROPERTY = "navigation_map_followed_name"
        private val CONTACT_EMAIL_PROPERTY = "navigation_map_followed_email"
        private val DEFAULT_ZOOM = 7.5f

        /** Length of time that will be taken for a double click.  */
        private val DOUBLE_CLICK_MS: Long = 1000
    }
}
