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
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_navigation.view.*
import pl.org.seva.navigator.R
import pl.org.seva.navigator.data.Contact
import pl.org.seva.navigator.data.ContactsStore
import pl.org.seva.navigator.listener.OnSwipeListener
import pl.org.seva.navigator.source.PeerLocationSource
import pl.org.seva.navigator.view.activity.NavigationActivity

fun navigationView(ctx: Context, f: NavigationViewHolder.() -> Unit): NavigationViewHolder =
        NavigationViewHolder(ctx).apply(f)

class NavigationViewHolder(ctx: Context): KodeinGlobalAware {

    private val peerLocationSource: PeerLocationSource = instance()
    private val store: ContactsStore = instance()

    private val TextView.hudSwipeListener get() = OnSwipeListener(ctx = context) {
        animate().alpha(0.0f).withEndAction { visibility = View.GONE }
        setOnTouchListener(null)
        stopWatchingPeer()
    }

    private val contactNameSpannable: CharSequence = ctx.getString(R.string.navigation_following_name).run {
        val idName = indexOf(CONTACT_NAME_PLACEHOLDER)
        val idEndName = idName + contact!!.name.length
        val boldSpan = StyleSpan(Typeface.BOLD)
        SpannableStringBuilder(replace(CONTACT_NAME_PLACEHOLDER, contact!!.name)).apply {
            setSpan(boldSpan, idName, idEndName, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    lateinit var view: View
    var contact: Contact? = null
        set(value: Contact?) {
            field = value
            field?.listen()
            updateHud()
        }

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
        contact = null
        peerLocationSource.clearPeerLocationListeners()
        clearMap()
    }

    private fun clearMap() = map!!.clear()

    private fun onPeerLocationReceived(latLng: LatLng) {
        latLng.putPeerMarker()
        peerLocation = latLng
        moveCamera()
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.onReady() {
        this@NavigationViewHolder.map = this.apply {
            setOnCameraIdleListener { onCameraIdle() }
            checkLocationPermission(
                    onGranted = { isMyLocationEnabled = true },
                    onDenied = {})
        }
        peerLocation?.putPeerMarker()
        moveCamera()
        moveCamera = this@NavigationActivity::moveCameraToPeerOrLastLocation
    }

    private fun LatLng.putPeerMarker() {
        map?.also {
            clearMap()
            it.addMarker(MarkerOptions()
                    .position(this)
                    ?.title(contact!!.name))
                    .setIcon(BitmapDescriptorFactory.defaultMarker(NavigationActivity.MARKER_HUE))
        }
    }

    private fun Contact.listen() {
        store.addContactsUpdatedListener(email, this@NavigationViewHolder::stopWatchingPeer)
        peerLocationSource.addPeerLocationListener(email, this@NavigationViewHolder::onPeerLocationReceived)
    }

    companion object {
        private val CONTACT_NAME_PLACEHOLDER = "[name]"
    }



}