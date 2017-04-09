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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import pl.org.seva.navigator.NavigatorApplication;
import pl.org.seva.navigator.R;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.model.ContactsCache;
import pl.org.seva.navigator.presenter.dagger.Graph;
import pl.org.seva.navigator.databinding.ActivityContactsBinding;
import pl.org.seva.navigator.presenter.listener.ContactClickListener;
import pl.org.seva.navigator.presenter.listener.ContactLongClickListener;
import pl.org.seva.navigator.presenter.listener.ContactsUpdatedListener;
import pl.org.seva.navigator.presenter.source.MyLocationSource;
import pl.org.seva.navigator.view.adapter.ContactAdapter;

public class ContactsActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ContactsUpdatedListener,
        ContactClickListener,
        ContactLongClickListener {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject MyLocationSource myLocationSource;
    @Inject
    ContactsCache contactsCache;

    private static final int PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID = 0;

    private ActivityContactsBinding binding;
    private RecyclerView contactsRecyclerView;
    private ContactAdapter contactAdapter;
    private boolean permissionAlreadyRequested;
    private Graph graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        graph = ((NavigatorApplication) getApplication()).getGraph();
        graph.inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
        contactsRecyclerView = binding.activityToolbar.contentContacts.contacts;

        Toolbar toolbar = binding.activityToolbar.appBarToolbar;
        setSupportActionBar(toolbar);
        binding.activityToolbar.fab
            .setOnClickListener(view -> startActivity(new Intent(this, SearchActivity.class)));

        DrawerLayout drawer = binding.drawerLayout;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.drawer_accessibility_open,
                R.string.drawer_accessibility_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        binding.navView.setNavigationItemSelectedListener(this);
        contactsCache.addContactsUpdatedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearDrawerSelection();
        View pleaseLogIn = binding.activityToolbar.contentContacts.pleaseLogIn;
        View header = binding.navView.getHeaderView(0);
        TextView name = ((TextView) header.findViewById(R.id.name));
        TextView email = ((TextView) header.findViewById(R.id.email));
        if (NavigatorApplication.isLoggedIn) {
            binding.activityToolbar.fab.setVisibility(View.VISIBLE);
            pleaseLogIn.setVisibility(View.GONE);
            contactsRecyclerView.setVisibility(View.VISIBLE);
            binding.navView.getMenu().findItem(R.id.drawer_login).setVisible(false);
            binding.navView.getMenu().findItem(R.id.drawer_logout).setVisible(true);
            name.setText(NavigatorApplication.displayName);
            email.setText(NavigatorApplication.email);
            alreadyLoggedIn();
        }
        else {
            pleaseLogIn.setVisibility(View.VISIBLE);
            contactsRecyclerView.setVisibility(View.GONE);
            binding.activityToolbar.fab.setVisibility(View.GONE);
            binding.navView.getMenu().findItem(R.id.drawer_login).setVisible(true);
            binding.navView.getMenu().findItem(R.id.drawer_logout).setVisible(false);
            name.setText(R.string.app_name);
            email.setText(R.string.drawer_header_not_logged_in);
            startLoginActivity();
        }
    }

    private void clearDrawerSelection() {
        Menu menu = binding.navView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
    }

    private void alreadyLoggedIn() {
        if (isLocationPermissionGranted()) {
            locationPermissionGranted();
        }
        else if (!permissionAlreadyRequested) {
            requestLocationPermission();
        }
    }

    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;
    }

    private void initContactsView() {
        contactsRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
        contactsRecyclerView.setLayoutManager(lm);
        contactAdapter = new ContactAdapter();
        graph.inject(contactAdapter);
        contactAdapter.addClickListener(this);
        contactAdapter.addLongClickListener(this);
        contactsRecyclerView.setAdapter(contactAdapter);
    }

    @Override
    public void onClick(Contact contact) {
        startActivity(new Intent(this, NavigationActivity.class).putExtra(NavigationActivity.CONTACT, contact));
    }

    @Override
    public void onLongClick(Contact contact) {

    }

    private void requestLocationPermission() {
        permissionAlreadyRequested = true;
        final String permissions[] = { Manifest.permission.ACCESS_FINE_LOCATION, };
        ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID) {
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted();
        }
        else {
            permissionDenied();
        }
    }

    private void locationPermissionGranted() {
        myLocationSource.connectGoogleApiClient();
        initContactsView();
    }

    private void permissionDenied() {
        Snackbar.make(
                binding.activityToolbar.contentContacts.contacts,
                R.string.permission_request_denied,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.permission_retry, view -> requestLocationPermission())
                .show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = binding.drawerLayout;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contacts_overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGIN));
    }

    private void logout() {
        startActivity(new Intent(this, LoginActivity.class)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGOUT));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.drawer_login:
                startLoginActivity();
            break;
            case R.id.drawer_logout:
                logout();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onContactsUpdated() {
        contactAdapter.notifyDataSetChanged();
    }
}
