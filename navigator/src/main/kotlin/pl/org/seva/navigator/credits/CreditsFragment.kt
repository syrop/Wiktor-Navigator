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

package pl.org.seva.navigator.credits

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_credits.*
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.ui.toast
import pl.org.seva.navigator.main.versionName

class CreditsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_credits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun String.inBrowser() {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(this)
            startActivity(i)
        }

        fun String.toClipboard() {
            val clipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText("", this)
            getString(R.string.credits_fragment_copied_to_clipboard).toast()
        }

        version.text = getString(R.string.credits_fragment_version)
                .replace(VERSION_PLACEHOLDER, versionName)

        developer_url.setOnClickListener {
            getString(R.string.credits_fragment_developer_url).inBrowser()
        }

        icon_github.setOnClickListener {
            getString(R.string.credits_fragment_icon_github).inBrowser()
        }

        developer_crypto_address.setOnClickListener {
            getString(R.string.credits_fragment_developer_btc).toClipboard()
        }
    }

    companion object {
        const val VERSION_PLACEHOLDER = "[version]"
    }
}
