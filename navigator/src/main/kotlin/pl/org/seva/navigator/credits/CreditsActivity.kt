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
 * If you like this program, consider donating bitcoin: 37vHXbpPcDBwcCTpZfjGL63JRwn6FPiXTS
 */

package pl.org.seva.navigator.credits

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.actility_credits.*
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.versionName

fun Context.creditsActivity(): Boolean {
    startActivity(Intent(this, CreditsActivity::class.java))
    return true
}

class CreditsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        fun String.inBrowser() {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(this)
            startActivity(i)
        }

        fun String.toClipboard() {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText("", this)
        }

        super.onCreate(savedInstanceState)

        setContentView(R.layout.actility_credits)
        version.text = getString(R.string.credits_activity_version)
                .replace(VERSION_PLACEHOLDER, versionName)

        developer_url.setOnClickListener {
            getString(R.string.credits_activity_developer_url).inBrowser()
        }

        icon_github.setOnClickListener {
            getString(R.string.credits_activity_icon_github).inBrowser()
        }

        developer_crypto_address.setOnClickListener {
            getString(R.string.credits_activity_developer_btc).toClipboard()
        }

        supportActionBar!!.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val VERSION_PLACEHOLDER = "[version]"
    }
}
