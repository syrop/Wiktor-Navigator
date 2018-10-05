/*
 * Copyright (C) 2018 Wiktor Nizio
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

package pl.org.seva.navigator.main

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_navigator.*
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.contactsStore
import pl.org.seva.navigator.contact.persist
import pl.org.seva.navigator.navigation.NavigationViewModel

class NavigatorActivity : AppCompatActivity() {

    private var backClickTime = 0L

    private var exitApplicationToast: Toast? = null

    private lateinit var navigationModel: NavigationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        navigationModel = ViewModelProviders.of(this).get(NavigationViewModel::class.java)
        setContentView(R.layout.activity_navigator)

        setSupportActionBar(toolbar)
        NavigationUI.setupActionBarWithNavController(
                this,
                findNavController(R.id.nav_host_fragment))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra(CONTACT_EMAIL_EXTRA)?.apply {
            val contact = contactsStore[this]
            navigationModel.contact.value = contact
            contact.persist()
        }
        if (Intent.ACTION_SEARCH == intent?.action) {
            navigationModel.query.value = intent.getStringExtra(SearchManager.QUERY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as NavigatorApplication).stopService()
    }

    override fun onSupportNavigateUp(): Boolean {
        val nc = findNavController(R.id.nav_host_fragment)
        return if (nc.currentDestination!!.id == R.id.navigationFragment &&
                System.currentTimeMillis() - backClickTime >= DOUBLE_CLICK_MS) {
            exitApplicationToast?.cancel()
            exitApplicationToast =
                    Toast.makeText(
                            this,
                            R.string.tap_back_second_time,
                            Toast.LENGTH_SHORT).apply { show() }
            backClickTime = System.currentTimeMillis()
            false
        } else {
            nc.navigateUp()
        }
    }

    companion object {
        /** Length of time that will be taken for a double click.  */
        private const val DOUBLE_CLICK_MS: Long = 1000

        const val CONTACT_EMAIL_EXTRA = "contact_email"
    }
}
