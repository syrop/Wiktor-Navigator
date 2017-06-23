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

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionsUtils @Inject
internal constructor() {
    private val permissionGrantedSubject = PublishSubject.create<PermissionResult>()
    private val permissionDeniedSubject = PublishSubject.create<PermissionResult>()

    fun request(
            activity: Activity,
            requestCode: Int,
            permissions: Array<PermissionRequest>) : Disposable {
        val permissionsToRequest = ArrayList<String>()
        val composite = CompositeDisposable()
        permissions.forEach { permission->
            permissionsToRequest.add(permission.permission)
            composite.addAll(
                    permissionGrantedSubject
                            .filter { it.requestCode == requestCode && it.permission == permission.permission }
                            .subscribe { permission.onGranted() },
                    permissionDeniedSubject
                            .filter { it.requestCode == requestCode && it.permission == permission.permission }
                            .subscribe { permission.onDenied() })
        }
        ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), requestCode)
        return composite
    }

    fun onRequestPermissionsResult(requestCode : Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isEmpty()) {
            for (permission in permissions) {
                onPermissionDenied(requestCode, permission)
            }
        }
        else for (i in 0 until permissions.size) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(requestCode, permissions[i])
            } else {
                onPermissionDenied(requestCode, permissions[i])
            }
        }
    }

    private fun onPermissionGranted(requestCode: Int, permission: String) {
        permissionGrantedSubject.onNext(PermissionResult(requestCode, permission))
    }

    private fun onPermissionDenied(requestCode: Int, permission: String) {
        permissionDeniedSubject.onNext(PermissionResult(requestCode, permission))
    }

    companion object {
        val LOCATION_PERMISSION_REQUEST_ID = 0
    }

    class PermissionResult(val requestCode: Int, val permission: String)
    class PermissionRequest(
            val permission: String,
            val onGranted: () -> Unit = {},
            val onDenied: () -> Unit = {})
}
