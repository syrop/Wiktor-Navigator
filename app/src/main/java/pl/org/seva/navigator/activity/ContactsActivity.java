package pl.org.seva.navigator.activity;

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

import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.R;
import pl.org.seva.navigator.databinding.ActivityContactsBinding;
import pl.org.seva.navigator.view.ContactAdapter;

public class ContactsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID = 0;

    private ActivityContactsBinding binding;
    private RecyclerView contactsRecyclerView;
    private boolean permissionAlreadyRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
        contactsRecyclerView = binding.toolbar.contentContacts.recyclerView;

        Toolbar toolbar = binding.toolbar.toolbar;
        setSupportActionBar(toolbar);
        binding.toolbar.fab
            .setOnClickListener(view -> Snackbar.make(
                    view,
                    "Replace with your own action",
                    Snackbar.LENGTH_LONG)
            .setAction("Action", null)
            .show());

        DrawerLayout drawer = binding.drawerLayout;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        binding.navView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearDrawerSelection();
        TextView pleaseLogIn = binding.toolbar.contentContacts.pleaseLogIn;
        View header = binding.navView.getHeaderView(0);
        TextView name = ((TextView) header.findViewById(R.id.name));
        TextView email = ((TextView) header.findViewById(R.id.email));

        if (NavigatorApplication.isLoggedIn) {
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
            binding.navView.getMenu().findItem(R.id.drawer_login).setVisible(true);
            binding.navView.getMenu().findItem(R.id.drawer_logout).setVisible(false);
            name.setText(R.string.app_name);
            email.setText(R.string.not_logged_in);
        }
    }

    private void clearDrawerSelection() {
        Menu menu = binding.navView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
    }

    private void alreadyLoggedIn() {
        if (isLocationPermissionEnabled()) {
            initContactsView();
        }
        else if (!permissionAlreadyRequested) {
            requestLocationPermission();
        }
    }

    private boolean isLocationPermissionEnabled() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;
    }

    private void initContactsView() {
        contactsRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
        contactsRecyclerView.setLayoutManager(lm);
        contactsRecyclerView.setAdapter(new ContactAdapter());
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
            permissionGranted();
        }
        else {
            permissionDenied();
        }
    }

    private void permissionGranted() {
        initContactsView();
    }

    private void permissionDenied() {
        Snackbar.make(
                binding.toolbar.contentContacts.recyclerView,
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.drawer_login) {
            startActivity(new Intent(this, GoogleSignInActivity.class)
                    .putExtra(GoogleSignInActivity.ACTION, GoogleSignInActivity.LOGIN));
        }
        else if (id == R.id.drawer_logout) {
            startActivity(new Intent(this, GoogleSignInActivity.class)
                    .putExtra(GoogleSignInActivity.ACTION, GoogleSignInActivity.LOGOUT));
        }
        else if (id == R.id.nav_slideshow) {

        }
        else if (id == R.id.nav_manage) {

        }
        else if (id == R.id.nav_share) {

        }
        else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
