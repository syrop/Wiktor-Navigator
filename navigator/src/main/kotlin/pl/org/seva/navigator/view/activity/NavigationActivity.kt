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
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
import pl.org.seva.navigator.listener.OnSwipeListener
import pl.org.seva.navigator.listener.Permissions
import pl.org.seva.navigator.source.PeerLocationSource
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
    private var map: GoogleMap? = null
    private var contact: Contact? = null
        set(value) {
            field = value
            value.persist()
        }
    private var permissionDisposable = Disposables.empty()
    private var isLocationPermissionGranted = false

    private var dialog: Dialog? = null
    private var snackbar: Snackbar? = null

    private var peerLocation: LatLng? = null

    private var animateCamera = true
    private var zoom = 0.0f
    private lateinit var lastCameraPosition: LatLng
    private var mapContainerId: Int = 0

    private var exitApplicationToast: Toast? = null

    private var moveCamera: () -> Unit = this::moveCameraToPeerOrLastLocation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val properties = PreferenceManager.getDefaultSharedPreferences(this)
        zoom = properties.getFloat(ZOOM_PROPERTY, DEFAULT_ZOOM)
        setContentView(R.layout.activity_navigation)
        lastCameraPosition = LatLng(properties.getFloat(LATITUDE_PROPERTY, 0.0f).toDouble(),
                properties.getFloat(LONGITUDE_PROPERTY, 0.0f).toDouble())
        supportActionBar?.title = getString(R.string.navigation_activity_label)
        if (savedInstanceState != null) {
            animateCamera = false
            peerLocation = savedInstanceState.getParcelable<LatLng?>(SAVED_PEER_LOCATION)
        }
        readContactFromProperties()
        contact?.listen()
        mapContainerId = map_container.id
        fab.setOnClickListener { onFabClicked() }
        updateHud()
        checkLocationPermission()
    }

    private fun Contact.listen() {
        store.addContactsUpdatedListener(email, this@NavigationActivity::stopWatchingPeer)
        peerLocationSource.addPeerLocationListener(email, this@NavigationActivity::onPeerLocationReceived)
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
            onReady()
        }
    }

    private fun LatLng.moveCamera() {
        val cameraPosition = CameraPosition.Builder().target(this).zoom(zoom).build()
        if (animateCamera) {
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            map?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        animateCamera = false
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

    private fun updateHud() = hud.run {
        alpha = 1.0f
        if (contact == null) {
            visibility = View.GONE
            setOnTouchListener(null)
        } else {
            visibility = View.VISIBLE
            text = contactNameSpannable
            setOnTouchListener(hudSwipeListener)
        }
    }

    private val TextView.hudSwipeListener get() = OnSwipeListener(this@NavigationActivity) {
        animate().alpha(0.0f).withEndAction { visibility = View.GONE }
        setOnTouchListener(null)
        stopWatchingPeer()
    }

    private val contactNameSpannable: CharSequence get() = getString(R.string.navigation_following_name).run {
        val idName = indexOf(CONTACT_NAME_PLACEHOLDER)
        val idEndName = idName + contact!!.name.length
        val boldSpan = StyleSpan(Typeface.BOLD)
        SpannableStringBuilder(replace(CONTACT_NAME_PLACEHOLDER, contact!!.name)).apply {
            setSpan(boldSpan, idName, idEndName, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    private fun onFabClicked() {
        stopWatchingPeer()
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
                    contact = data?.getParcelableExtra(CONTACT_IN_INTENT)
                    contact?.listen()
                }
                updateHud()
            }
            DELETE_PROFILE_REQUEST_ID -> {
                if (resultCode == Activity.RESULT_OK) {
                    deleteProfile()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.onReady() {
        this@NavigationActivity.map = this.apply {
            setOnCameraIdleListener { onCameraIdle() }
            checkLocationPermission(
                    onGranted = { isMyLocationEnabled = true },
                    onDenied = {})
        }
        peerLocation?.putPeerMarker()
        moveCamera()
        moveCamera = this@NavigationActivity::moveCameraToPeerOrLastLocation
    }

    private fun moveCameraToPeerOrLastLocation() = (peerLocation?:lastCameraPosition).moveCamera()

    private fun moveCameraToLast() = lastCameraPosition.moveCamera()

    inline private fun checkLocationPermission(
            onGranted: () -> Unit = this::onLocationPermissionGranted ,
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
        map?.isMyLocationEnabled = true
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
        stopWatchingPeer()
        startActivity(Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGOUT))
    }

    private fun deleteProfile() {
        stopWatchingPeer()
        store.clear()
        instance<ContactsDatabase>().contactDao.deleteAll()
        fbWriter.deleteMe()
        logout()
    }

    private fun onDeleteProfileClicked() {
        val intent = Intent(this, DeleteProfileActivity::class.java)
        startActivityForResult(intent, DELETE_PROFILE_REQUEST_ID)
    }

    private fun onCameraIdle() = map!!.cameraPosition.let {
            zoom = it.zoom
            lastCameraPosition = it.target
            if (lastCameraPosition different peerLocation) {
                moveCamera = this::moveCameraToLast
            }
            persistCameraPositionAndZoom()
        }

    private infix fun LatLng.different(other: LatLng?): Boolean {
        if (other == null) return true
        return Math.abs(latitude - other.latitude) > FLOAT_TOLERANCE ||
                Math.abs(longitude - other.longitude) > FLOAT_TOLERANCE
    }

    @SuppressLint("CommitPrefEdits")
    private fun persistCameraPositionAndZoom() =
            with (PreferenceManager.getDefaultSharedPreferences(this).edit()) {
        putFloat(ZOOM_PROPERTY, zoom)
        putFloat(LATITUDE_PROPERTY, lastCameraPosition.latitude.toFloat())
        putFloat(LONGITUDE_PROPERTY, lastCameraPosition.longitude.toFloat())
        apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        deleteMapFragment()
        outState.putParcelable(SAVED_PEER_LOCATION, peerLocation)
        super.onSaveInstanceState(outState)
    }

    private fun deleteMapFragment() {
        mapFragment?.apply {
            fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            mapFragment = null
        }
    }

    private fun onPeerLocationReceived(latLng: LatLng) {
        latLng.putPeerMarker()
        peerLocation = latLng
        moveCamera()
    }

    private fun stopWatchingPeer() {
        contact = null
        peerLocationSource.clearPeerLocationListeners()
        clearMap()
    }

    private fun clearMap() = map!!.clear()

    private fun LatLng.putPeerMarker() {
        map?.also {
            clearMap()
            it.addMarker(MarkerOptions()
                    .position(this)
                    ?.title(contact!!.name))
                    .setIcon(BitmapDescriptorFactory.defaultMarker(MARKER_HUE))
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

        private val CONTACT_NAME_PLACEHOLDER = "[name]"

        private val DELETE_PROFILE_REQUEST_ID = 0
        private val CONTACTS_ACTIVITY_REQUEST_ID = 1

        /** Calculated from #00bfa5, or A700 Teal. */
        private val MARKER_HUE = 34.0f

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

        private val FLOAT_TOLERANCE = 0.002f
    }
}
