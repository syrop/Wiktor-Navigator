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

package pl.org.seva.navigator.main.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.coroutineScope
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import pl.org.seva.navigator.main.init.instance

val activityRecognition by instance<ActivityRecognitionObservable>()

open class ActivityRecognitionObservable {

    private var initialized = false
    private var state = STATIONARY
    private lateinit var context: Context
    private var activityRecognitionReceiver: BroadcastReceiver? = null

    private val stateFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private val mutableStateLiveData = MutableLiveData<Int>()

    val stateLiveData: LiveData<Int> = mutableStateLiveData

    @SuppressLint("MissingPermission")
    infix fun init(context: Context) {
        if (initialized) {
            return
        }
        this.context = context
        registerReceiver()
        val intent = Intent(ACTIVITY_RECOGNITION_INTENT).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        ActivityRecognition.getClient(context).requestActivityUpdates(
            ACTIVITY_RECOGNITION_INTERVAL_MS,
            pendingIntent,
        )
        initialized = true
    }

    private fun registerReceiver() {
        activityRecognitionReceiver = activityRecognitionReceiver ?: ActivityRecognitionReceiver()
        ContextCompat.registerReceiver(
            context,
            activityRecognitionReceiver,
            IntentFilter(ACTIVITY_RECOGNITION_INTENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun listen(lifecycle: Lifecycle, f: (Int) -> Unit) {
        f(state)
        lifecycle.coroutineScope.launch {
            stateFlow.collect { f(state) }
        }
    }

    private fun onDeviceStationary() {
        state = STATIONARY
        stateFlow.tryEmit(STATIONARY)
        mutableStateLiveData.value = STATIONARY
    }

    private fun onDeviceMoving() {
        state = MOVING
        stateFlow.tryEmit(MOVING)
        mutableStateLiveData.value = MOVING
    }

    private inner class ActivityRecognitionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)
                if (result?.probably(DetectedActivity.STILL) == true) {
                    onDeviceStationary()
                } else {
                    onDeviceMoving()
                }
            }
        }

        private infix fun ActivityRecognitionResult.probably(activity: Int) =
            (mostProbableActivity.type == activity) && (getActivityConfidence(activity) >= MIN_CONFIDENCE)
    }

    companion object {

        const val STATIONARY = 0
        const val MOVING = 1

        private const val ACTIVITY_RECOGNITION_INTENT = "activity_recognition_intent"
        private const val ACTIVITY_RECOGNITION_INTERVAL_MS = 1000L
        /** The device is only stationary if confidence >= this level. */
        private const val MIN_CONFIDENCE = 80 // 70 does not work on LG G6 Android 7.0
    }
}
