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
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.disposables.Disposables
import kotlinx.android.synthetic.main.activity_navigation.*
import org.apache.commons.io.IOUtils

import javax.inject.Inject

import pl.org.seva.navigator.R
import pl.org.seva.navigator.NavigatorApplication
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.Login
import pl.org.seva.navigator.model.firebase.FirebaseWriter
import pl.org.seva.navigator.model.sqlite.SqlWriter
import pl.org.seva.navigator.presenter.OnSwipeListener
import pl.org.seva.navigator.presenter.PermissionsUtils
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.source.PeerLocationSource
import java.io.IOException

class NavigationActivity : AppCompatActivity() {

    @Inject
    lateinit var peerLocationSource: PeerLocationSource
    @Inject
    lateinit var contactsStore: ContactsStore
    @Inject
    lateinit var permissionsUtils: PermissionsUtils
    @Inject
    lateinit var myLocationSource: MyLocationSource
    @Inject
    lateinit var login: Login
    @Inject
    lateinit var sqlWriter: SqlWriter
    @Inject
    lateinit var firebaseWriter: FirebaseWriter

    /** Used when counting a double click.  */
    private var clickTime: Long = 0

    private var mapFragment: MapFragment? = null
    private var map: GoogleMap? = null
    private var contact: Contact? = null
    private var permissionDisposable = Disposables.empty()
    private var isLocationPermissionGranted = false

    private var dialog: Dialog? = null
    private var snackbar: Snackbar? = null

    private var peerLocation: LatLng? = null

    private var animateCamera = true
    private var zoom = 0.0f
    private lateinit var target: LatLng
    private var mapContainerId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val properties = PreferenceManager.getDefaultSharedPreferences(this)
        zoom = properties.getFloat(ZOOM_PROPERTY, DEFAULT_ZOOM)
        target = LatLng(properties.getFloat(LATITUDE_PROPERTY, 0.0f).toDouble(),
                properties.getFloat(LONGITUDE_PROPERTY, 0.0f).toDouble())

        supportActionBar?.title = getString(R.string.navigation_activity_label)
        savedInstanceState?.let {
            animateCamera = false
            peerLocation = savedInstanceState.getParcelable<LatLng?>(SAVED_PEER_LOCATION)
            if (peerLocation != null) { moveCameraToPeerLocation() }
        }

        (application as NavigatorApplication).component.inject(this)
        setContentView(R.layout.activity_navigation)

        contact = intent.getParcelableExtra<Contact>(CONTACT)
        contact?.let {
            contactsStore.addContactsUpdatedListener(it.email!!, { stopWatchingPeer() })
        }
        mapContainerId = map_container.id
        fab.setOnClickListener { onFabClicked() }
        updateHud()
        checkLocationPermission()
    }

    private fun updateHud() {
        hud.alpha = 1.0f
        if (contact == null) {
            hud.visibility = View.GONE
            hud.setOnTouchListener(null)
        } else {
            hud.visibility = View.VISIBLE
            hud.text = contactNameCharSequence()
            hud.setOnTouchListener(
                    OnSwipeListener(this, onLeft = { onHudSwiped() } , onRight = { onHudSwiped() } ))
        }
    }

    private fun onHudSwiped() {
        hud.animate().alpha(0.0f).withEndAction { hud.visibility = View.GONE }
        hud.setOnTouchListener(null)
        stopWatchingPeer()
    }

    private fun contactNameCharSequence() : CharSequence {
        val str = getString(R.string.navigation_following_name)
        val idName = str.indexOf(CONTACT_NAME_PLACEHOLDER)
        val idEndName = idName + contact!!.name!!.length
        val ssBuilder = SpannableStringBuilder(str.replace(CONTACT_NAME_PLACEHOLDER, contact!!.name!!))
        val boldSpan = StyleSpan(Typeface.BOLD)
        ssBuilder.setSpan(boldSpan, idName, idEndName, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return ssBuilder
    }

    private fun onFabClicked() {
        stopWatchingPeer()
        if (!isLocationPermissionGranted) {
            checkLocationPermission()
        } else if (login.isLoggedIn) {
            startActivityForResult(Intent(this, ContactsActivity::class.java), CONTACTS_ACTIVITY_REQUEST_ID)
        } else {
            showLoginSnackbar()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CONTACTS_ACTIVITY_REQUEST_ID -> {
                if (resultCode == Activity.RESULT_OK) {
                    contact = data?.getParcelableExtra(CONTACT)
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

    private fun moveCameraToPeerLocation() {
        val cameraPosition = CameraPosition.Builder().target(peerLocation).zoom(zoom).build()
        if (animateCamera) {
            map!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        animateCamera = false
    }

    @SuppressLint("MissingPermission")
    private fun onGoogleMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.setOnCameraIdleListener { onCameraIdle() }
        checkLocationPermission(
                onGranted = { map?.isMyLocationEnabled = true },
                onDenied = {})
        contact?.let {
            peerLocationSource.addPeerLocationListener(it.email!!, { onPeerLocationReceived(it) })
        }
        val cameraPosition = CameraPosition.Builder().target(target).zoom(zoom).build()
        map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun checkLocationPermission(
            onGranted : (() -> Unit)? = { onLocationPermissionGranted() },
            onDenied : (() -> Unit)? = { requestLocationPermission() } ) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionGranted = true
            onGranted?.invoke()
        } else {
            onDenied?.invoke()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.navigation_overflow_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_help).isVisible = !isLocationPermissionGranted || !login.isLoggedIn
        menu.findItem(R.id.action_logout).isVisible = login.isLoggedIn
        menu.findItem(R.id.action_delete_user).isVisible = login.isLoggedIn
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                logout()
                return true
            }
            R.id.action_delete_user -> {
                onDeleteProfileClicked()
                return true
            }
            R.id.action_help -> {
                if (!isLocationPermissionGranted) {
                    showLocationPermissionHelp()
                } else if (!login.isLoggedIn) {
                    showLoginHelp()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLocationPermissionHelp() {
        showHelp(R.layout.dialog_help_location_permission, HELP_LOCATION_PERMISSION_EN) {
            onSettingsClicked()
        }
    }

    private fun showLoginHelp() {
        showHelp(R.layout.dialog_help_login, HELP_LOGIN_EN) {
            login()
        }
    }

    private fun showHelp(layout : Int, file: String, action: () -> Unit) {
        dialog = Dialog(this)
        dialog!!.setContentView(layout)
        val web = dialog!!.findViewById<WebView>(R.id.web)
        web.settings.defaultTextEncodingName = UTF_8

        try {
            val content = IOUtils.toString(assets.open(file), UTF_8)
                    .replace(APP_VERSION_PLACEHOLDER, versionName)
                    .replace(APP_NAME_PLACEHOLDER, getString(R.string.app_name))
            web.loadDataWithBaseURL(ASSET_DIR, content, PLAIN_TEXT, UTF_8, null)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        dialog!!.findViewById<View>(R.id.action_button).setOnClickListener { action() }
        dialog!!.show()
    }

    private val versionName: String
        get() {
            try {
                return packageManager.getPackageInfo(packageName, 0).versionName
            } catch (ex: PackageManager.NameNotFoundException) {
                return ""
            }
        }

    private fun onSettingsClicked() {
        dialog?.dismiss()
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun requestLocationPermission() {
        permissionDisposable = permissionsUtils.request(
                this,
                PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID,
                arrayOf(PermissionsUtils.PermissionRequest(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        onGranted = { onLocationPermissionGranted() },
                        onDenied = { onLocationPermissionDenied() })))
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
                .setAction(R.string.snackbar_retry) { requestLocationPermission() }
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
            grantResults: IntArray) {
        permissionsUtils.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

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
        contactsStore.clear()
        sqlWriter.deleteAllFriends()
        firebaseWriter.deleteMe()
        logout()
    }

    private fun onDeleteProfileClicked() {
        val intent = Intent(this, DeleteProfileActivity::class.java)
        startActivityForResult(intent, DELETE_PROFILE_REQUEST_ID)
    }

    private fun onCameraIdle() {
        val cameraPosition = map!!.cameraPosition
        zoom = cameraPosition.zoom
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putFloat(ZOOM_PROPERTY, zoom)
        target = cameraPosition.target
        editor.putFloat(LATITUDE_PROPERTY, target.latitude.toFloat())
        editor.putFloat(LONGITUDE_PROPERTY, target.longitude.toFloat())
        editor.apply()
    }

    override fun onPause() {
        super.onPause()
        peerLocationSource.clearPeerLocationListeners()
    }

    override fun onResume() {
        super.onResume()
        checkLocationPermission(
                onGranted = {
                    snackbar?.dismiss()
                    if (!login.isLoggedIn) {
                        showLoginSnackbar()
                    }},
                onDenied = {})
        invalidateOptionsMenu()
        prepareMapFragment()
    }

    private fun prepareMapFragment() {
        val fm = fragmentManager
        mapFragment = fm.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
        mapFragment?:let {
            mapFragment = MapFragment()
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_FRAGMENT_TAG).commit()
            mapFragment!!.getMapAsync( { onGoogleMapReady(it) })
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        deleteMapFragment()
        outState.putParcelable(SAVED_PEER_LOCATION, peerLocation)
        super.onSaveInstanceState(outState)
    }

    private fun deleteMapFragment() {
        mapFragment?.let {
            fragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            mapFragment = null
        }
    }

    private fun onPeerLocationReceived(latLng: LatLng) {
        peerLocation = latLng
        putPeerMarkerOnMap()
        moveCameraToPeerLocation()
    }

    private fun stopWatchingPeer() {
        contact = null
        peerLocationSource.clearPeerLocationListeners()
        clearMap()
    }

    private fun clearMap() {
        map!!.clear()
    }

    private fun putPeerMarkerOnMap() {
        map?.let {
            clearMap()
            it.addMarker(MarkerOptions()
                    .position(peerLocation!!)
                    .title(contact!!.name!!))
                    .setIcon(BitmapDescriptorFactory.defaultMarker(MARKER_HUE))
        }
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - clickTime < DOUBLE_CLICK_MS) {
            (application as NavigatorApplication).stopService()
            super.onBackPressed()
        } else {
            Toast.makeText(this, R.string.tap_back_second_time, Toast.LENGTH_SHORT).show()
            clickTime = System.currentTimeMillis()
        }
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

        val CONTACT = "contact"

        private val MAP_FRAGMENT_TAG = "map"

        private val SAVED_PEER_LOCATION = "saved_peer_location"

        private val ZOOM_PROPERTY = "navigation_map_zoom"
        private val LATITUDE_PROPERTY = "navigation_map_latitude"
        private val LONGITUDE_PROPERTY = "navigation_map_longitude"
        private val DEFAULT_ZOOM = 7.5f

        /** Length of time that will be taken for a double click.  */
        private val DOUBLE_CLICK_MS: Long = 5000
    }
}
