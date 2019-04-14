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
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.navigator.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fr_delete_profile.*
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.extension.back
import pl.org.seva.navigator.main.extension.inflate
import pl.org.seva.navigator.main.extension.viewModel

class DeleteProfileFragment : Fragment() {

    private val navigatorModel by viewModel<NavigatorViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflate(R.layout.fr_delete_profile, container)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ok.setOnClickListener {
            navigatorModel.deleteProfile.value = true
            back()
        }
        cancel.setOnClickListener {
            navigatorModel.deleteProfile.value = false
            back()
        }
    }
}
