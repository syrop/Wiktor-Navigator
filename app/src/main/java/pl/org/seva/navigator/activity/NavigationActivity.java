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

package pl.org.seva.navigator.activity;

import android.app.FragmentManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.databinding.ActivityNavigationBinding;

@SuppressWarnings("MissingPermission")
public class NavigationActivity extends AppCompatActivity {

    private static final String MAP_FRAGMENT_TAG = "map";

    private MapFragment mapFragment;
    private GoogleMap map;

    ActivityNavigationBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

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
    protected void onSaveInstanceState(Bundle outState) {
        if (mapFragment != null) {
            // see http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit#10261449
            getFragmentManager().beginTransaction().remove(mapFragment).commitAllowingStateLoss();
            mapFragment = null;
        }
        super.onSaveInstanceState(outState);
    }

}
