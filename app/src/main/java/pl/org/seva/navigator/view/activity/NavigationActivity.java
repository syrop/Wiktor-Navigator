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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import javax.inject.Inject;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.NavigatorApplication;
import pl.org.seva.navigator.databinding.ActivityNavigationBinding;
import pl.org.seva.navigator.presenter.receiver.PeerLocationReceiver;
import pl.org.seva.navigator.presenter.source.PeerLocationSource;

@SuppressWarnings("MissingPermission")
public class NavigationActivity extends AppCompatActivity implements PeerLocationReceiver {

    public static final String EMAIL = "email";

    @Inject PeerLocationSource peerLocationSource;

    private static final String MAP_FRAGMENT_TAG = "map";

    private MapFragment mapFragment;
    private GoogleMap map;
    private String email;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((NavigatorApplication) getApplication()).getGraph().inject(this);
        ActivityNavigationBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);
        email = getIntent().getStringExtra(EMAIL);

        int mapContainerId = binding.toolbar.contentNavigation.mapContainer.getId();

        FragmentManager fm = getFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapFragment == null) {
            mapFragment = new MapFragment();
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_FRAGMENT_TAG).commit();
        }

        mapFragment.getMapAsync(googleMap -> {
            map = googleMap;
            map.setMyLocationEnabled(true);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (email != null) {
            peerLocationSource.addPeerLocationReceiver(email, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        peerLocationSource.clearPeerLocationReceivers();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mapFragment != null) {
            // see http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit#10261449
            getFragmentManager().beginTransaction().remove(mapFragment).commitAllowingStateLoss();
            mapFragment = null;
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPeerLocationReceived(LatLng latLng) {

    }
}
