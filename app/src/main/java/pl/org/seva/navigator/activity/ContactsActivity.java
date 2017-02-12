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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import pl.org.seva.navigator.NavigatorApplication;
import pl.org.seva.navigator.R;
import pl.org.seva.navigator.databinding.ActivityContactsBinding;

public class ContactsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID = 0;

    private ActivityContactsBinding binding;
    private RecyclerView contactsRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
        TextView pleaseLogIn = binding.toolbar.contentContacts.pleaseLogIn;
        contactsRecyclerView = binding.toolbar.contentContacts.recyclerView;

        Toolbar toolbar = binding.toolbar.toolbar;
        setSupportActionBar(toolbar);
        binding.toolbar.fab
            .setOnClickListener(view -> Snackbar.make(
                    view,
                    "Replace with your own action",
                    Snackbar.LENGTH_LONG)
            .setAction("Action", null).show());

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

        if (NavigatorApplication.isLoggedIn) {
            pleaseLogIn.setVisibility(View.GONE);
            contactsRecyclerView.setVisibility(View.VISIBLE);
            alreadyLoggedIn();
        }
        else {
            pleaseLogIn.setVisibility(View.VISIBLE);
            contactsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void alreadyLoggedIn() {
        if (isLocationPermissionEnabled()) {
            initContactsView();
        }
        else {
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

    }

    private void requestLocationPermission() {
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
            initContactsView();
        }
        else {
            Snackbar.make(
                    binding.toolbar.contentContacts.recyclerView,
                    "Camera permission request was denied.",
                    Snackbar.LENGTH_SHORT).show();
        }
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
        getMenuInflater().inflate(R.menu.contacts_menu, menu);
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

        if (id == R.id.login) {
            startActivity(new Intent(this, GoogleSignInActivity.class));
        }
        else if (id == R.id.nav_gallery) {

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
