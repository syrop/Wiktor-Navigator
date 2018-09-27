package pl.org.seva.navigator.main

import android.content.Intent
import android.os.Bundle
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

    private val navigationModel =
            ViewModelProviders.of(this).get(NavigationViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_main)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra(NavigationFragment.CONTACT_EMAIL_EXTRA)?.apply {
            val contact = contactsStore[this]
            navigationModel.contact.value = contact
            contact.persist()
        }
    }

    override fun onSupportNavigateUp()
            = findNavController(R.id.nav_host_fragment).navigateUp()
}
