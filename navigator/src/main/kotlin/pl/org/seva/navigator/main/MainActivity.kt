package pl.org.seva.navigator.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.contactsStore
import pl.org.seva.navigator.contact.persist
import pl.org.seva.navigator.navigation.NavigationFragment
import pl.org.seva.navigator.navigation.NavigationViewModel

class MainActivity : AppCompatActivity() {

    private var backClickTime = 0L

    private var exitApplicationToast: Toast? = null

    private val navigationModel =
            ViewModelProviders.of(this).get(NavigationViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_main)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra(CONTACT_EMAIL_EXTRA)?.apply {
            val contact = contactsStore[this]
            navigationModel.contact.value = contact
            contact.persist()
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
