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

package pl.org.seva.navigator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.graphics.drawable.Drawable
import android.view.View

class DividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private var divider: Drawable

    init {
        val a = context.obtainStyledAttributes(ATTRS)
        divider = a.getDrawable(0)
        a.recycle()
    }

    override fun onDrawOver(c: Canvas?, parent: RecyclerView, state: RecyclerView.State?) {

        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight
            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
        outRect.set(0, 0, 0, divider.intrinsicHeight)
    }

    companion object {
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }
}
