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

package pl.org.seva.navigator.presenter

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

class ContactTouchHelperCallback (val onItemSwiped: (Int) -> Unit) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(p0: RecyclerView?, p1: RecyclerView.ViewHolder?) =
            makeMovementFlags(0, ItemTouchHelper.START or ItemTouchHelper.END)

    override fun onMove(p0: RecyclerView?, p1: RecyclerView.ViewHolder?,
                        p2: RecyclerView.ViewHolder?) = false

    override fun onSwiped(p0: RecyclerView.ViewHolder?, p1: Int) {
        onItemSwiped(p1)
    }

    override fun isLongPressDragEnabled() = false
}
