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
import android.arch.lifecycle.LifecycleActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.View

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import javax.inject.Inject

import pl.org.seva.navigator.R
import pl.org.seva.navigator.NavigatorApplication
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.ContactsCache
import pl.org.seva.navigator.presenter.PermissionsUtils
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.source.PeerLocationSource

class NavigationActivity : LifecycleActivity() {

    @Inject
    lateinit var peerLocationSource: PeerLocationSource
    @Inject
    lateinit var contactsCache: ContactsCache
    @Inject
    lateinit var permissionsUtils: PermissionsUtils
    @Inject
    lateinit var myLocationSource: MyLocationSource

    private var mapFragment: MapFragment? = null
    private var map: GoogleMap? = null
    private var contact: Contact? = null

    private val fab by lazy { findViewById<View>(R.id.fab) }
    private val mapContainer by lazy { findViewById<View>(R.id.map_container) }

    private var peerLocation: LatLng? = null

    private var animateCamera = true
    private var moveCameraToPeerLocation = true
    private var zoom = 0.0f
    private var mapContainerId: Int = 0
    private var locationPermissionGranted = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        zoom = PreferenceManager.getDefaultSharedPreferences(this)
                .getFloat(ZOOM_PROPERTY_NAME, DEFAULT_ZOOM)
        savedInstanceState?.let {
            animateCamera = false
            peerLocation = savedInstanceState.getParcelable<LatLng>(SAVED_PEER_LOCATION)
            peerLocation?.let {  moveCameraToPeerLocation() }
        }

        (application as NavigatorApplication).component.inject(this)
        setContentView(R.layout.activity_navigation)

        contact = intent.getParcelableExtra<Contact>(CONTACT)
        contact?.let {
            contactsCache.addContactsUpdatedListener(it.email(), { this.onContactsUpdated() })
        }
        mapContainerId = mapContainer.id
        fab.setOnClickListener { onFabClicked() }
    }

    private fun onFabClicked() {
        startActivity(Intent(this, ContactsActivity::class.java))
    }

    private fun moveCameraToPeerLocation() {
        val cameraPosition = CameraPosition.Builder()
                .target(peerLocation).zoom(zoom).build()
        if (animateCamera) {
            map!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        moveCameraToPeerLocation = false
        animateCamera = false
    }

    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.setOnCameraIdleListener { onCameraIdle() }
        processLocationPermission()
        contact?.let {
            peerLocationSource.addPeerLocationListener(it.email(), { this.onPeerLocationReceived(it) })
        }
    }

    private fun processLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted()
        } else {
            requestLocationPermission()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.navigation_overflow_menu, menu)
        return NavigatorApplication.isLoggedIn
    }

    private fun requestLocationPermission() {
        permissionsUtils.request(
                this,
                PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID,
                arrayOf(PermissionsUtils.PermissionRequest(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        grantedListener = { onLocationPermissionGranted() },
                        deniedListener = { onLocationPermissionDenied() })))
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        locationPermissionGranted = true
        myLocationSource.onLocationGranted(applicationContext)
        map?.isMyLocationEnabled = true
    }

    private fun onLocationPermissionDenied() {
        Snackbar.make(
                mapContainer,
                R.string.permission_request_denied,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.permission_retry) { requestLocationPermission() }
                .show()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        permissionsUtils.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun logout() {
        startActivity(Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGOUT))
    }

    private fun onCameraIdle() {
        if (!moveCameraToPeerLocation) {
            zoom = map!!.cameraPosition.zoom
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putFloat(ZOOM_PROPERTY_NAME, zoom).apply()
    }

    override fun onPause() {
        super.onPause()
        peerLocationSource.clearPeerLocationListeners()
    }

    override fun onResume() {
        super.onResume()
        prepareMapFragment()
    }

    private fun prepareMapFragment() {
        val fm = fragmentManager
        mapFragment = fm.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
        mapFragment?:let {
            mapFragment = MapFragment()
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_FRAGMENT_TAG).commit()
        }
        mapFragment!!.getMapAsync( { onMapReady(it) })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        deleteMapFragment()
        outState.putParcelable(SAVED_PEER_LOCATION, peerLocation)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        deleteMapFragment()
        super.onDestroy()
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
        if (moveCameraToPeerLocation) {
            moveCameraToPeerLocation()
        }
    }

    private fun onContactsUpdated() {
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
                    .title(contact!!.name()))
                    .setIcon(BitmapDescriptorFactory.defaultMarker(MARKER_HUE))
        }
    }

    companion object {

        /** Calculated from #00bfa5, or A700 Teal. */
        private val MARKER_HUE = 34.0f

        val CONTACT = "contact"

        private val MAP_FRAGMENT_TAG = "map"

        private val SAVED_PEER_LOCATION = "saved_peer_location"

        private val ZOOM_PROPERTY_NAME = "navigation_map_zoom"
        private val DEFAULT_ZOOM = 7.5f
    }
}
