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
 *
 * If you like this program, consider donating bitcoin: 3JVNWUeVH118S3pzU4hDgkUNwEeNarZySf
 */

package pl.org.seva.navigator.profile

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_delete_user.*
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.startForResult

fun FragmentActivity.deleteProfileActivity(requestCode: Int): Boolean {
    startForResult(DeleteProfileActivity::class.java, requestCode)
    return true
}

class DeleteProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_delete_user)
        ok.setOnClickListener { onOkClicked() }
        cancel.setOnClickListener { onCancelClicked() }
    }

    private fun onOkClicked() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun onCancelClicked() {
        finish()
    }
}
