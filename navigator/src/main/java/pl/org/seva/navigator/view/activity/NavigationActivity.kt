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

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

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
import pl.org.seva.navigator.source.PeerLocationSource

class NavigationActivity : AppCompatActivity() {

    @Inject
    lateinit var peerLocationSource: PeerLocationSource
    @Inject
    lateinit var contactsCache: ContactsCache

    private var mapFragment: MapFragment? = null
    private var map: GoogleMap? = null
    private var contact: Contact? = null

    private var peerLocation: LatLng? = null

    private var animateCamera = true
    private var moveCameraToPeerLocation = true
    private var zoom = 0.0f
    private var mapContainerId: Int = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        zoom = PreferenceManager.getDefaultSharedPreferences(this)
                .getFloat(ZOOM_PROPERTY_NAME, DEFAULT_ZOOM)
        savedInstanceState?.let {
            animateCamera = false
            peerLocation = savedInstanceState.getParcelable<LatLng>(SAVED_PEER_LOCATION)
            peerLocation?.let {  moveCameraToPeerLocation() }
        }

        (application as NavigatorApplication).graph.inject(this)
        setContentView(R.layout.activity_navigation)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        contact = intent.getParcelableExtra<Contact>(CONTACT)
        contact?.let {
            contactsCache.addContactsUpdatedListener(it.email(), { this.onContactsUpdated() })
        }
        mapContainerId = findViewById(R.id.map_container).id
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

    @SuppressLint("MissingPermission")
    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.isMyLocationEnabled = true
        map!!.setOnCameraIdleListener { onCameraIdle() }
        contact?.let {
            peerLocationSource.addPeerLocationListener(it.email(), { this.onPeerLocationReceived(it) })
        }
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
        val fm = fragmentManager
        mapFragment = fm.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
        if (mapFragment == null) {
            mapFragment = MapFragment()
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_FRAGMENT_TAG).commit()
        }
        mapFragment!!.getMapAsync( { this.onMapReady(it) })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapFragment?.let {
            // see http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit#10261449
            fragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            mapFragment = null
        }
        outState.putParcelable(SAVED_PEER_LOCATION, peerLocation)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
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
        if (map == null) {
            return
        }
        clearMap()
        map!!.addMarker(MarkerOptions()
                .position(peerLocation!!)
                .title(contact!!.name()))
                .setIcon(BitmapDescriptorFactory.defaultMarker(MARKER_HUE))
    }

    companion object {

        private val MARKER_HUE = 34.0f  // calculated from #00bfa5

        val CONTACT = "contact"

        private val MAP_FRAGMENT_TAG = "map"

        private val SAVED_PEER_LOCATION = "saved_peer_location"

        private val ZOOM_PROPERTY_NAME = "navigation_map_zoom"
        private val DEFAULT_ZOOM = 7.5f
    }
}
