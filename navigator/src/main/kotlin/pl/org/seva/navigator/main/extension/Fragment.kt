/*
 * Copyright (C) 2019 Wiktor Nizio
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

package pl.org.seva.navigator.main.extension

import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import org.kodein.di.LazyDelegate
import kotlin.reflect.KProperty

fun Fragment.navigate(@IdRes resId: Int): Boolean {
    findNavController().navigate(resId)
    return true
}

fun Fragment.back() = findNavController().popBackStack()

inline fun <reified R : ViewModel> Fragment.viewModel() = object : LazyDelegate<R> {
    override fun provideDelegate(receiver: Any?, prop: KProperty<Any?>) = lazy {
        ViewModelProviders.of(this@viewModel.activity!!).get(R::class.java)
    }
}

fun Fragment.inflate(@LayoutRes resource: Int, root: ViewGroup?): View =
    layoutInflater.inflate(resource, root, false)
