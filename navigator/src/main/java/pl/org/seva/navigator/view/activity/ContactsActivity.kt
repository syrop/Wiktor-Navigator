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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import javax.inject.Inject

import pl.org.seva.navigator.NavigatorApplication
import pl.org.seva.navigator.R
import pl.org.seva.navigator.model.Contact
import pl.org.seva.navigator.model.ContactsCache
import pl.org.seva.navigator.NavigatorComponent
import pl.org.seva.navigator.model.database.firebase.FirebaseWriter
import pl.org.seva.navigator.source.MyLocationSource
import pl.org.seva.navigator.view.adapter.ContactAdapter
import pl.org.seva.navigator.view.builder.dialog.FriendshipDeleteDialogBuilder

class ContactsActivity : AppCompatActivity() {

    @Inject
    lateinit var myLocationSource: MyLocationSource
    @Inject
    lateinit var contactsCache: ContactsCache
    @Inject
    lateinit var firebaseWriter: FirebaseWriter

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private var permissionAlreadyRequested: Boolean = false
    private lateinit var graph: NavigatorComponent
    private lateinit var navigationView: NavigationView
    private lateinit var fab: View
    private lateinit var drawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graph = (application as NavigatorApplication).graph
        graph.inject(this)
        setContentView(R.layout.activity_contacts)
        contactsRecyclerView = findViewById<RecyclerView>(R.id.contacts)

        val toolbar = findViewById<Toolbar>(R.id.app_bar_toolbar)
        setSupportActionBar(toolbar)
        fab = findViewById<View>(R.id.fab)
        fab.setOnClickListener { _ -> startActivity(Intent(this, SearchActivity::class.java)) }

        drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.drawer_accessibility_open,
                R.string.drawer_accessibility_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { this.onNavigationItemSelected(it) }
        contactsCache.addContactsUpdatedListener { this.onContactsUpdated() }
    }

    override fun onResume() {
        super.onResume()
        clearDrawerSelection()
        val pleaseLogIn = findViewById<View>(R.id.please_log_in)

        val header = navigationView.getHeaderView(0)
        val name = header.findViewById<TextView>(R.id.name)
        val email = header.findViewById<TextView>(R.id.email)
        if (NavigatorApplication.isLoggedIn) {
            fab.visibility = View.VISIBLE
            pleaseLogIn.visibility = View.GONE
            contactsRecyclerView.visibility = View.VISIBLE
            navigationView.menu.findItem(R.id.drawer_login).isVisible = false
            navigationView.menu.findItem(R.id.drawer_logout).isVisible = true
            name.text = NavigatorApplication.displayName
            email.text = NavigatorApplication.email
            alreadyLoggedIn()
        } else {
            pleaseLogIn.visibility = View.VISIBLE
            contactsRecyclerView.visibility = View.GONE
            fab.visibility = View.GONE
            navigationView.menu.findItem(R.id.drawer_login).isVisible = true
            navigationView.menu.findItem(R.id.drawer_logout).isVisible = false
            name.setText(R.string.app_name)
            email.setText(R.string.drawer_header_not_logged_in)
            startLoginActivity()
        }
    }

    private fun clearDrawerSelection() {
        val menu = navigationView.menu
        for (i in 0..menu.size() - 1) {
            menu.getItem(i).isChecked = false
        }
    }

    private fun alreadyLoggedIn() {
        if (isLocationPermissionGranted) {
            locationPermissionGranted()
        } else if (!permissionAlreadyRequested) {
            requestLocationPermission()
        }
    }

    private val isLocationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun initContactsRecyclerView() {
        contactsRecyclerView.setHasFixedSize(true)
        val lm = LinearLayoutManager(this)
        contactsRecyclerView.layoutManager = lm
        contactAdapter = ContactAdapter()
        graph.inject(contactAdapter)
        contactAdapter.addClickListener { this.onContactClicked(it) }
        contactAdapter.addLongClickListener { this.onContactLongClicked(it) }
        contactsRecyclerView.adapter = contactAdapter
    }

    private fun onContactClicked(contact: Contact) {
        val intent = Intent(this, NavigationActivity::class.java)

        if (contact.email() != NavigatorApplication.email) {
            intent.putExtra(NavigationActivity.CONTACT, contact)
        }
        startActivity(intent)
    }

    private fun onContactLongClicked(contact: Contact) {
        FriendshipDeleteDialogBuilder(this)
                .setContact(contact)
                .setOnConfirmedAction { onDeleteFriendConfirmed(contact) }
                .build()
                .show()
    }

    private fun onDeleteFriendConfirmed(contact: Contact) {
        firebaseWriter.deleteFriendship(contact)
        contactsCache.delete(contact)
        contactAdapter.notifyDataSetChanged()
    }

    private fun requestLocationPermission() {
        permissionAlreadyRequested = true
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        if (requestCode != PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID) {
            return
        }

        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted()
        } else {
            permissionDenied()
        }
    }

    private fun locationPermissionGranted() {
        myLocationSource.connectGoogleApiClient()
        initContactsRecyclerView()
    }

    private fun permissionDenied() {
        Snackbar.make(
                contactsRecyclerView,
                R.string.permission_request_denied,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.permission_retry) { _ -> requestLocationPermission() }
                .show()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun startLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGIN))
    }

    private fun logout() {
        startActivity(Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.ACTION, LoginActivity.LOGOUT))
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.drawer_login -> startLoginActivity()
            R.id.drawer_logout -> logout()
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun onContactsUpdated() {
        contactAdapter.notifyDataSetChanged()
    }

    companion object {

        private val PERMISSION_ACCESS_FINE_LOCATION_REQUEST_ID = 0
    }
}
