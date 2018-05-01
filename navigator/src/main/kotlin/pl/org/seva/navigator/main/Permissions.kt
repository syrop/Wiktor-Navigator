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

package pl.org.seva.navigator.main

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

fun permissions() = instance<Permissions>()

class Permissions {

    private val grantedSubject = PublishSubject.create<PermissionResult>()
    private val deniedSubject = PublishSubject.create<PermissionResult>()
    private val composite = CompositeDisposable()

    fun request(
            activity: AppCompatActivity,
            requestCode: Int,
            permissions: Array<PermissionRequest>) {
        val permissionsToRequest = ArrayList<String>()
        permissions.forEach { permission ->
            permissionsToRequest.add(permission.permission)
            composite.addAll(
                    grantedSubject
                            .filter { it.requestCode == requestCode && it.permission == permission.permission }
                            .subscribe { permission.onGranted() },
                    deniedSubject
                            .filter { it.requestCode == requestCode && it.permission == permission.permission }
                            .subscribe { permission.onDenied() })
        }
        activity.lifecycle.addObserver(PermissionObserver())
        ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), requestCode)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        infix fun String.onGranted(requestCode: Int) =
                grantedSubject.onNext(PermissionResult(requestCode, this))

        infix fun String.onDenied(requestCode: Int) =
                deniedSubject.onNext(PermissionResult(requestCode, this))

        if (grantResults.isEmpty()) {
            permissions.forEach { it onDenied requestCode }
        } else repeat(permissions.size) {
            if (grantResults[it] == PackageManager.PERMISSION_GRANTED) {
                permissions[it] onGranted requestCode
            } else {
                permissions[it] onDenied requestCode
            }
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_ID = 0
    }

    data class PermissionResult(val requestCode: Int, val permission: String)

    class PermissionRequest(
            val permission: String,
            val onGranted: () -> Unit = {},
            val onDenied: () -> Unit = {})

    inner class PermissionObserver : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            composite.dispose()
        }
    }
}
