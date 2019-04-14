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

//https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures

package pl.org.seva.navigator.main.view

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

class OnHudSwipeListener(ctx: Context, val onSwiped: (() -> Unit)) : OnTouchListener {

    private val gestureDetector: GestureDetector
    private var x = -1
    private var animating = false

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }

    override fun onTouch(view: View, ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> if (!animating) x = ev.x.toInt()
            MotionEvent.ACTION_MOVE -> if (x >= 0) view.x = (ev.x.toInt() - x).toFloat()
            MotionEvent.ACTION_UP -> if (x >= 0) animateBack(view)
        }
        return gestureDetector.onTouchEvent(ev)
    }

    private fun animateBack(v : View) {
        x = -1
        animating = true
        v.animate().setDuration(125L).x(0.0f).withEndAction { animating = false }
    }

    private inner class GestureListener : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent) = true

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            var result = false
            val dX = e2.x - e1.x
            val dY = e2.y - e1.y
            if (Math.abs(dX) > Math.abs(dY)) {
                if (Math.abs(dX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    onSwiped.invoke()
                    result = true
                }
            }

            return result
        }
    }

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}
