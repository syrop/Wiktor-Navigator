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

package pl.org.seva.navigator.main

import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import io.reactivex.subjects.PublishSubject

fun Fragment.requestPermissions(
        requestCode: Int,
        requests: Array<Permissions.PermissionRequest>) =
    permissions.request(
            this,
            requestCode,
            requests)

val permissions by instance<Permissions>()

class Permissions {

    private val grantedSubject = PublishSubject.create<PermissionResult>()
    private val deniedSubject = PublishSubject.create<PermissionResult>()

    fun request(
            fragment: Fragment,
            requestCode: Int,
            requests: Array<PermissionRequest>) {
        val lifecycle = fragment.lifecycle
        val permissionsToRequest = ArrayList<String>()
        requests.forEach { request ->
            permissionsToRequest.add(request.permission)
                grantedSubject
                        .filter { it.requestCode == requestCode && it.permission == request.permission }
                        .subscribe(lifecycle) { request.onGranted() }
                deniedSubject
                        .filter { it.requestCode == requestCode && it.permission == request.permission }
                        .subscribe(lifecycle) { request.onDenied() }
        }
        fragment.requestPermissions(permissionsToRequest.toTypedArray(), requestCode)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        infix fun String.onGranted(requestCode: Int) =
                grantedSubject.onNext(PermissionResult(requestCode, this))

        infix fun String.onDenied(requestCode: Int) =
                deniedSubject.onNext(PermissionResult(requestCode, this))

        if (grantResults.isEmpty()) {
            permissions.forEach { it onDenied requestCode }
        }
        else repeat(permissions.size) {
            if (grantResults[it] == PackageManager.PERMISSION_GRANTED) {
                permissions[it] onGranted requestCode
            }
            else {
                permissions[it] onDenied requestCode
            }
        }
    }

    companion object {
        const val DEFAULT_PERMISSION_REQUEST_ID = 0
    }

    data class PermissionResult(val requestCode: Int, val permission: String)

    class PermissionRequest(
            val permission: String,
            val onGranted: () -> Unit = {},
            val onDenied: () -> Unit = {})
}
