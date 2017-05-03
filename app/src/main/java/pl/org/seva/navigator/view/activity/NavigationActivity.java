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

package pl.org.seva.navigator.view.activity;

import android.app.FragmentManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import javax.inject.Inject;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.NavigatorApplication;
import pl.org.seva.navigator.databinding.ActivityNavigationBinding;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.model.ContactsCache;
import pl.org.seva.navigator.source.PeerLocationSource;

@SuppressWarnings("MissingPermission")
public class NavigationActivity extends AppCompatActivity {

    private static final float MARKER_HUE = 34.0f;  // calculated from #00bfa5

    public static final String CONTACT = "contact";

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject PeerLocationSource peerLocationSource;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject ContactsCache contactsCache;

    private static final String MAP_FRAGMENT_TAG = "map";

    private MapFragment mapFragment;
    private GoogleMap map;
    private Contact contact;
    private ActivityNavigationBinding binding;

    private LatLng peerLocation;

    private static final String SAVED_PEER_LOCATION = "saved_peer_location";

    private static final String ZOOM_PROPERTY_NAME = "navigation_map_zoom";
    private static final float DEFAULT_ZOOM = 7.5f;

    private boolean animateCamera = true;
    private boolean moveCameraToPeerLocation = true;
    private float zoom;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        zoom = PreferenceManager.getDefaultSharedPreferences(this)
                .getFloat(ZOOM_PROPERTY_NAME, DEFAULT_ZOOM);
        if (savedInstanceState != null) {
            animateCamera = false;
            peerLocation = savedInstanceState.getParcelable(SAVED_PEER_LOCATION);
            if (peerLocation != null) {
                moveCameraToPeerLocation();
            }
        }

        ((NavigatorApplication) getApplication()).getGraph().inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        contact = getIntent().getParcelableExtra(CONTACT);
        if (contact != null) {
            contactsCache.addContactsUpdatedListener(contact.email(), this::onContactsUpdated);
        }
    }

    private void moveCameraToPeerLocation() {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(peerLocation).zoom(zoom).build();
        if (animateCamera) {
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        moveCameraToPeerLocation = false;
        animateCamera = false;
    }

    private void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMyLocationEnabled(true);
        map.setOnCameraIdleListener(this::onCameraIdle);
        if (contact != null) {
            peerLocationSource.addPeerLocationListener(contact.email(), this::onPeerLocationReceived);
        }
    }

    private void onCameraIdle() {
        if (!moveCameraToPeerLocation) {
            zoom = map.getCameraPosition().zoom;
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().
                putFloat(ZOOM_PROPERTY_NAME, zoom).apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        peerLocationSource.clearPeerLocationListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int mapContainerId = binding.mapContainer.getId();
        FragmentManager fm = getFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapFragment == null) {
            mapFragment = new MapFragment();
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_FRAGMENT_TAG).commit();
        }
        mapFragment.getMapAsync(this::onMapReady);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mapFragment != null) //noinspection SpellCheckingInspection
        {
            // see http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit#10261449
            getFragmentManager().beginTransaction().remove(mapFragment).commitAllowingStateLoss();
            mapFragment = null;
        }
        outState.putParcelable(SAVED_PEER_LOCATION, peerLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onPeerLocationReceived(LatLng latLng) {
        peerLocation = latLng;
        putPeerMarkerOnMap();
        if (moveCameraToPeerLocation) {
            moveCameraToPeerLocation();
        }
    }

    private void onContactsUpdated() {
        contact = null;
        peerLocationSource.clearPeerLocationListeners();
        clearMap();
    }

    private void clearMap() {
        map.clear();
    }

    private void putPeerMarkerOnMap() {
        if (map == null) {
            return;
        }
        clearMap();
        map.addMarker(new MarkerOptions()
                .position(peerLocation)
                .title(contact.name()))
                .setIcon(BitmapDescriptorFactory.defaultMarker(MARKER_HUE));
    }
}
