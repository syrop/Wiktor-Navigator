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

package pl.org.seva.navigator.view.activity.viewholder

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_navigation.view.*
import pl.org.seva.navigator.data.model.Contact
import pl.org.seva.navigator.data.ContactsStore
import pl.org.seva.navigator.listener.OnSwipeListener
import pl.org.seva.navigator.source.PeerLocationSource

fun navigationView(f: NavigationViewHolder.() -> Unit): NavigationViewHolder =
        NavigationViewHolder().apply(f)

class NavigationViewHolder: KodeinGlobalAware {

    private val peerLocationSource: PeerLocationSource = instance()
    private val store: ContactsStore = instance()

    private var map: GoogleMap? = null
    var peerLocation: LatLng? = null

    private var moveCamera: () -> Unit = this::moveCameraToPeerOrLastLocation

    lateinit var lastCameraPosition: LatLng

    var animateCamera = true
    var zoom = 0.0f

    lateinit var checkLocationPermission: (f: () -> Unit) -> Unit
    lateinit var persistCameraPositionAndZoom: () -> Unit
    lateinit var deletePersistedContact: () -> Unit

    private val TextView.hudSwipeListener get() = OnSwipeListener(ctx = context) {
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

    lateinit var view: View
    var contact: Contact? = null
        set(value) {
            field = value
            field?.listen()
            updateHud()
        }
    lateinit var contactNameTemplate: String

    fun updateHud() = view.hud.run {
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

    fun stopWatchingPeer() {
        peerLocationSource.clearPeerLocationListeners()
        peerLocation = null
        contact = null
        clearMap()
    }

    private fun clearMap() = map!!.clear()

    private fun onPeerLocationReceived(latLng: LatLng) {
        peerLocation = latLng
        latLng.putPeerMarker()
        moveCamera()
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

    private fun moveCameraToPeerOrLastLocation() = (peerLocation?:lastCameraPosition).moveCamera()

    private fun moveCameraToLast() = lastCameraPosition.moveCamera()

    infix fun ready(map: GoogleMap) = map.onReady()

    @SuppressLint("MissingPermission")
    fun locationPermissionGranted() {
        map?.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.onReady() {
        this@NavigationViewHolder.map = this.apply {
            setOnCameraIdleListener { onCameraIdle() }
            checkLocationPermission { isMyLocationEnabled = true }
        }
        peerLocation?.putPeerMarker()
        moveCamera()
        moveCamera = this@NavigationViewHolder::moveCameraToPeerOrLastLocation
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
        store.addContactsUpdatedListener(email, this@NavigationViewHolder::stopWatchingPeer)
        peerLocationSource.addPeerLocationListener(email, this@NavigationViewHolder::onPeerLocationReceived)
    }

    companion object {

        private val CONTACT_NAME_PLACEHOLDER = "[name]"

        private val FLOAT_TOLERANCE = 0.002f
        /** Calculated from #00bfa5, or A700 Teal. */
        private val MARKER_HUE = 34.0f
    }
}
