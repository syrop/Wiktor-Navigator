/*
 * Copyright (C) 2026 Wiktor Nizio
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

package pl.org.seva.navigator.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.abs
import kotlinx.coroutines.launch
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.contact.contacts
import pl.org.seva.navigator.contact.persist
import pl.org.seva.navigator.contact.readContactFromProperties
import pl.org.seva.navigator.debug.debug
import pl.org.seva.navigator.main.NavigatorApplication
import pl.org.seva.navigator.main.NavigatorViewModel
import pl.org.seva.navigator.main.data.ActivityRecognitionObservable
import pl.org.seva.navigator.main.data.activityRecognition
import pl.org.seva.navigator.main.data.db.contactDao
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.data.setShortcuts
import pl.org.seva.navigator.main.extension.prefs
import pl.org.seva.navigator.main.extension.readString
import pl.org.seva.navigator.main.extension.toast
import pl.org.seva.navigator.main.init.APP_VERSION
import pl.org.seva.navigator.main.init.instance
import pl.org.seva.navigator.profile.LoginActivity
import pl.org.seva.navigator.profile.isLoggedIn
import pl.org.seva.navigator.profile.loginActivity

@Composable
fun NavigationRoute(
    navigatorModel: NavigatorViewModel,
    onNavigateToContacts: () -> Unit,
    isLocationPermissionGranted: Boolean,
    onLocationPermissionChanged: (Boolean) -> Unit,
    helpTrigger: Int,
    logoutTrigger: Int,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.prefs }
    val versionName by instance<String>(APP_VERSION)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isStationary by remember { mutableStateOf(value = false) }
    var followingContact by remember {
        mutableStateOf(
            value = navigatorModel.contact.value ?: readContactFromProperties(prefs),
        )
    }
    var peerLocation by rememberSaveable { mutableStateOf<LatLng?>(value = null) }
    var helpDialogParams by remember { mutableStateOf<HelpDialogParams?>(value = null) }
    var animateCamera by remember { mutableStateOf(value = true) }
    var moveCameraToPeer by remember { mutableStateOf(value = true) }

    val initialLatLng = remember {
        LatLng(
            prefs.getFloat(LATITUDE_PROPERTY, 0.0f).toDouble(),
            prefs.getFloat(LONGITUDE_PROPERTY, 0.0f).toDouble(),
        )
    }
    val initialZoom = remember {
        prefs.getFloat(ZOOM_PROPERTY, DEFAULT_ZOOM)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            peerLocation ?: initialLatLng,
            initialZoom,
        )
    }

    fun checkHasPermission(): Boolean =
        (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED)

    fun login() {
        helpDialogParams = null
        context.loginActivity(LoginActivity.LOGIN)
    }

    fun showLoginSnackbar() {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.snackbar_please_log_in),
                actionLabel = context.getString(R.string.snackbar_login),
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) {
                login()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onLocationPermissionChanged(granted)
        if (granted) {
            snackbarHostState.currentSnackbarData?.dismiss()
            if (isLoggedIn) {
                (context.applicationContext as NavigatorApplication).startService()
            } else {
                showLoginSnackbar()
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snackbar_permission_request_denied),
                    actionLabel = context.getString(R.string.snackbar_retry),
                    duration = SnackbarDuration.Indefinite,
                )
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    onLocationPermissionChanged(checkHasPermission())
                }
            }
        }
    }

    fun requestLocationPermissions() {
        if (checkHasPermission()) {
            onLocationPermissionChanged(true)
            if (isLoggedIn) {
                (context.applicationContext as NavigatorApplication).startService()
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    fun stopWatchingPeer() {
        peerObservable.clearPeerListeners()
        peerLocation = null
        followingContact = null
        navigatorModel.contact.value = null
    }

    fun logout() {
        null persist prefs
        stopWatchingPeer()
        context.loginActivity(LoginActivity.LOGOUT)
    }

    // Observe camera idle & persist position
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val pos = cameraPositionState.position
            prefs.edit {
                putFloat(ZOOM_PROPERTY, pos.zoom)
                putFloat(LATITUDE_PROPERTY, pos.target.latitude.toFloat())
                putFloat(LONGITUDE_PROPERTY, pos.target.longitude.toFloat())
            }
            val currentPeer = peerLocation
            if (currentPeer != null) {
                val isDiff = (abs(pos.target.latitude - currentPeer.latitude) > FLOAT_TOLERANCE) ||
                    (abs(pos.target.longitude - currentPeer.longitude) > FLOAT_TOLERANCE)
                if (isDiff) {
                    moveCameraToPeer = false
                }
            }
        }
    }

    // Move camera when peerLocation updates
    LaunchedEffect(peerLocation) {
        val target = peerLocation ?: return@LaunchedEffect
        if (moveCameraToPeer) {
            val update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(target)
                    .zoom(cameraPositionState.position.zoom)
                    .build(),
            )
            if (animateCamera) {
                cameraPositionState.animate(update)
                animateCamera = false
            } else {
                cameraPositionState.move(update)
            }
        }
    }

    // Observe contact changes & subscribe to peer location
    DisposableEffect(followingContact) {
        peerObservable.clearPeerListeners()
        val contact = followingContact
        contact persist prefs
        if (contact != null) {
            moveCameraToPeer = true
            contacts.addContactsUpdatedListener(contact.email) {
                stopWatchingPeer()
            }
            peerObservable.addLocationObserver(contact.email) { latLng ->
                peerLocation = latLng
            }
            if (debug.isDebugMode) {
                peerObservable.addDebugObserver(contact.email) { message ->
                    message.toast()
                }
            }
        }
        onDispose {
            peerObservable.clearPeerListeners()
        }
    }

    // Observe ViewModel & ActivityRecognition LiveData
    DisposableEffect(lifecycleOwner) {
        val stateObserver = Observer<Int> { state ->
            isStationary = (state == ActivityRecognitionObservable.STATIONARY)
        }
        val contactObserver = Observer<Contact?> { contact ->
            if (contact != null) {
                followingContact = contact
            }
        }
        val deleteObserver = Observer<Boolean> { shouldDelete ->
            if (shouldDelete == true) {
                stopWatchingPeer()
                contacts.clear()
                contactDao.deleteAll()
                setShortcuts()
                fbWriter.deleteMe()
                logout()
                navigatorModel.deleteProfile.value = false
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = checkHasPermission()
                onLocationPermissionChanged(granted)
                if (granted) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    if (isLoggedIn) {
                        (context.applicationContext as NavigatorApplication).startService()
                    } else {
                        showLoginSnackbar()
                    }
                }
            }
        }

        activityRecognition.stateLiveData.observe(lifecycleOwner, stateObserver)
        navigatorModel.contact.observe(lifecycleOwner, contactObserver)
        navigatorModel.deleteProfile.observe(lifecycleOwner, deleteObserver)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            activityRecognition.stateLiveData.removeObserver(stateObserver)
            navigatorModel.contact.removeObserver(contactObserver)
            navigatorModel.deleteProfile.removeObserver(deleteObserver)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(Unit) {
        requestLocationPermissions()
    }

    LaunchedEffect(helpTrigger) {
        if (helpTrigger > 0) {
            fun openHelp(captionRes: Int, file: String, action: () -> Unit) {
                val content = context.assets.open(file).readString()
                    .replace(APP_VERSION_PLACEHOLDER, versionName)
                    .replace(APP_NAME_PLACEHOLDER, context.getString(R.string.app_name))
                helpDialogParams = HelpDialogParams(
                    htmlContent = content,
                    actionButtonText = context.getString(captionRes),
                    onActionButtonClick = action,
                )
            }

            if (!isLocationPermissionGranted) {
                openHelp(
                    R.string.dialog_settings_button,
                    HELP_LOCATION_PERMISSION_EN,
                ) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:" + context.packageName)
                    }
                    context.startActivity(intent)
                }
            } else if (!isLoggedIn) {
                openHelp(
                    R.string.dialog_login_button,
                    HELP_LOGIN_EN,
                    action = ::login,
                )
            }
        }
    }

    LaunchedEffect(logoutTrigger) {
        if (logoutTrigger > 0) {
            logout()
        }
    }

    Surface {
        NavigationScreen(
            isStationary = isStationary,
            followingContact = followingContact,
            peerLocation = peerLocation,
            isLocationPermissionGranted = isLocationPermissionGranted,
            cameraPositionState = cameraPositionState,
            snackbarHostState = snackbarHostState,
            onAddContactClick = {
                stopWatchingPeer()
                if (!isLocationPermissionGranted) {
                    requestLocationPermissions()
                } else if (isLoggedIn) {
                    onNavigateToContacts()
                } else {
                    showLoginSnackbar()
                }
            },
            onDismissFollowing = {
                stopWatchingPeer()
                null persist prefs
            },
            helpDialogParams = helpDialogParams,
            onDismissHelpDialog = { helpDialogParams = null },
        )
    }
}

private const val APP_VERSION_PLACEHOLDER = "[app_version]"
private const val APP_NAME_PLACEHOLDER = "[app_name]"
private const val HELP_LOCATION_PERMISSION_EN = "help_location_permission_en.html"
private const val HELP_LOGIN_EN = "help_login_en.html"
private const val FLOAT_TOLERANCE = 0.002f

const val ZOOM_PROPERTY = "navigation_map_zoom"
const val LATITUDE_PROPERTY = "navigation_map_latitude"
const val LONGITUDE_PROPERTY = "navigation_map_longitude"
const val DEFAULT_ZOOM = 7.5f
