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

package pl.org.seva.navigator.model

import android.os.Parcel
import android.os.Parcelable

class ParcelableInt(val value: Int) : Parcelable {
    companion object {
        @JvmField val CREATOR: Parcelable.Creator<ParcelableInt> = object : Parcelable.Creator<ParcelableInt> {
            override fun createFromParcel(source: Parcel): ParcelableInt = ParcelableInt(source)
            override fun newArray(size: Int): Array<ParcelableInt?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
    source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(value)
    }
}
