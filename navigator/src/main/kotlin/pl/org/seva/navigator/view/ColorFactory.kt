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

package pl.org.seva.navigator.view

import android.app.Application
import android.graphics.Color

class ColorFactory(private val application: Application ) {

    private val colors by lazy {
        application.run {
            resources.getIdentifier(COLOR_ARRAY_NAME + COLOR_TYPE,"array", packageName).let {
                resources.obtainTypedArray(it)
            }
        }
    }

    fun nextColor() = with(colors) {
        val index = (Math.random() * length()).toInt()
        getColor(index, Color.GRAY)
    }

    companion object {
        val COLOR_ARRAY_NAME = "mdcolor_"
        val COLOR_TYPE = "400"
    }
}
