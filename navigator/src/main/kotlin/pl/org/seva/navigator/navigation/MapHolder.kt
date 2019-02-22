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

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_navigation.view.*
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.*
import pl.org.seva.navigator.debug.isDebugMode
import pl.org.seva.navigator.main.applicationContext
import pl.org.seva.navigator.main.extension.toast
import pl.org.seva.navigator.main.prefs
import pl.org.seva.navigator.main.ui.OnHudSwipeListener

fun Fragment.createMapHolder(f: MapHolder.() -> Unit): MapHolder = MapHolder().apply(f).also {
    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync { map -> it withMap map }
}

class MapHolder {

    private var map: GoogleMap? = null
    var peerLocation: LatLng? = null

    private var moveCamera: () -> Unit = ::moveCameraToPeerOrLastLocation

    lateinit var lastCameraPosition: LatLng

    private var animateCamera = true
    var zoom = 0.0f

    lateinit var checkLocationPermission: (f: () -> Unit) -> Unit
    lateinit var persistCameraPositionAndZoom: () -> Unit
    private lateinit var deletePersistedContact: () -> Unit

    private val TextView.hudSwipeListener get() = OnHudSwipeListener(ctx = context) {
        animate().alpha(0.0f).withEndAction { visibility = View.GONE }
        setOnTouchListener(null)
        stopWatchingPeer()
        deletePersistedContact()
    }

    private val contactNameSpannable: CharSequence get() = contactNameTemplate.run {
        val idName = indexOf(CONTACT_NAME_PLACEHOLDER)
        val idEndName = idName + contact!!.name.length
        val boldSpan = StyleSpan(Typeface.BOLD)
        SpannableStringBuilder(replace(CONTACT_NAME_PLACEHOLDER, contact!!.name)).apply {
            setSpan(boldSpan, idName, idEndName, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    lateinit var view: ViewGroup

    var contact: Contact? = null
        set(value) {
            field = value
            field?.listen()
            updateHud()
        }

    private lateinit var contactNameTemplate: String

    fun init(savedInstanceState: Bundle?, root: ViewGroup, contact: Contact?) {
        view = root
        zoom = prefs.getFloat(NavigationFragment.ZOOM_PROPERTY, NavigationFragment.DEFAULT_ZOOM)
        lastCameraPosition = LatLng(prefs.getFloat(NavigationFragment.LATITUDE_PROPERTY, 0.0f).toDouble(),
                prefs.getFloat(NavigationFragment.LONGITUDE_PROPERTY, 0.0f).toDouble())
        contactNameTemplate = applicationContext.getString(R.string.navigation_following_name)

        contact?.persist()
        if (contact == null) {
            this.contact = readContactFromProperties()
        }

        deletePersistedContact = { null.persist() }
        if (savedInstanceState != null) {
            animateCamera = false
            peerLocation = savedInstanceState.getParcelable<LatLng?>(NavigationFragment.SAVED_PEER_LOCATION)
        }
    }

    private fun updateHud() = view.hud_following.run {
        alpha = 1.0f
        if (contact == null) {
            animate().translationYBy(height.toFloat()).withEndAction { visibility = View.GONE }
            setOnTouchListener(null)
        }
        else {
            visibility = View.VISIBLE
            animate().translationYBy(0.0f).withEndAction { translationY = 0.0f }
            text = contactNameSpannable
            setOnTouchListener(hudSwipeListener)
        }
    }

    fun stopWatchingPeer() {
        peerObservable.clearPeerListeners()
        peerLocation = null
        contact = null
        clearMap()
    }

    private fun clearMap() = map?.clear()

    private fun LatLng.moveCamera() {
        val cameraPosition = CameraPosition.Builder().target(this).zoom(zoom).build()
        if (animateCamera) {
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        else {
            map?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        animateCamera = false
    }

    private fun moveCameraToPeerOrLastLocation() = (peerLocation?:lastCameraPosition).moveCamera()

    private fun moveCameraToLast() = lastCameraPosition.moveCamera()

    infix fun withMap(map: GoogleMap) = map.onReady()

    @SuppressLint("MissingPermission")
    fun locationPermissionGranted() {
        map?.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.onReady() {
        infix fun LatLng.isDifferentFrom(other: LatLng?) = if (other == null) true
        else Math.abs(latitude - other.latitude) > FLOAT_TOLERANCE ||
                Math.abs(longitude - other.longitude) > FLOAT_TOLERANCE

        fun onCameraIdle() = map!!.cameraPosition.let {
            zoom = it.zoom
            lastCameraPosition = it.target
            if (lastCameraPosition isDifferentFrom peerLocation) {
                moveCamera = ::moveCameraToLast
            }
            persistCameraPositionAndZoom()
        }

        this@MapHolder.map = apply {
            setOnCameraIdleListener { onCameraIdle() }
            checkLocationPermission { isMyLocationEnabled = true }
        }
        peerLocation?.putPeerMarker()
        moveCamera()
        moveCamera = this@MapHolder::moveCameraToPeerOrLastLocation
    }

    private fun LatLng.putPeerMarker() {
        map?.also {
            clearMap()
            it.addMarker(MarkerOptions()
                    .position(this)
                    .title(contact!!.name))
                    .setIcon(BitmapDescriptorFactory.defaultMarker(MARKER_HUE))
        }
    }

    private fun Contact.listen() {
        fun onPeerLocationReceived(latLng: LatLng) {
            peerLocation = latLng
            latLng.putPeerMarker()
            moveCamera()
        }

        contacts.addContactsUpdatedListener(email, this@MapHolder::stopWatchingPeer)
        peerObservable.addLocationListener(email)  { onPeerLocationReceived(it) }
        if (isDebugMode) {
            peerObservable.addDebugListener(email)  { message -> message.toast() }
        }
    }

    companion object {
        private const val CONTACT_NAME_PLACEHOLDER = "[name]"

        private const val FLOAT_TOLERANCE = 0.002f
        /** Calculated from #00bfa5, or A700 Teal. */
        private const val MARKER_HUE = 34.0f
    }
}
