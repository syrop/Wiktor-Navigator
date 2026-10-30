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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.Contact
import pl.org.seva.navigator.main.ui.NavigatorTheme

private val HudBackgroundColor = Color(0x99000000)

/** Calculated from #00bfa5, or A700 Teal. */
const val MARKER_HUE = 34.0f

data class HelpDialogParams(
    val htmlContent: String,
    val actionButtonText: String,
    val onActionButtonClick: () -> Unit,
)

/**
 * Jetpack Compose screen for navigation and peer tracking on Google Maps.
 *
 * Layout mapping:
 * - Root `Scaffold` with [SnackbarHost] and `Box(Modifier.fillMaxSize())`.
 * - Map slot (`mapContent: @Composable () -> Unit`) hosting [GoogleMap] with peer [Marker].
 * - FAB at bottom-end (`FloatingActionButton`) with `add` icon.
 * - Stationary HUD banner at the top (`hud_stationary`, `#99000000` background, 16sp, 8dp padding).
 * - Following contact HUD banner at the bottom (`hud_following`, `#99000000` background, 16sp, 8dp padding)
 *   with swipe-to-dismiss support triggering [onDismissFollowing].
 * - Help dialog hoisted via [helpDialogParams] and [onDismissHelpDialog].
 */
@Composable
fun NavigationScreen(
    isStationary: Boolean,
    followingContact: Contact?,
    onAddContactClick: () -> Unit,
    onDismissFollowing: () -> Unit,
    modifier: Modifier = Modifier,
    peerLocation: LatLng? = null,
    isLocationPermissionGranted: Boolean = false,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    helpDialogParams: HelpDialogParams? = null,
    onDismissHelpDialog: () -> Unit = {},
    mapContent: @Composable () -> Unit = {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = isLocationPermissionGranted,
            ),
        ) {
            if (peerLocation != null && followingContact != null) {
                Marker(
                    state = remember(peerLocation) { MarkerState(position = peerLocation) },
                    title = followingContact.name,
                    icon = BitmapDescriptorFactory.defaultMarker(MARKER_HUE),
                )
            }
        }
    },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            mapContent()

            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (followingContact != null) 64.dp else 16.dp,
                    ),
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = stringResource(id = R.string.action_search),
                )
            }

            AnimatedVisibility(
                visible = isStationary,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Text(
                    text = stringResource(id = R.string.device_not_moving),
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HudBackgroundColor)
                        .padding(8.dp),
                )
            }

            AnimatedVisibility(
                visible = followingContact != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                if (followingContact != null) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if ((dismissValue == SwipeToDismissBoxValue.StartToEnd) ||
                                (dismissValue == SwipeToDismissBoxValue.EndToStart)
                            ) {
                                onDismissFollowing()
                                true
                            } else {
                                false
                            }
                        },
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        content = {
                            val template = stringResource(id = R.string.navigation_following_name)
                            val placeholder = "[name]"
                            val placeholderIndex = template.indexOf(placeholder)
                            val annotatedString = buildAnnotatedString {
                                if (placeholderIndex >= 0) {
                                    append(template.substring(0, placeholderIndex))
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(followingContact.name)
                                    }
                                    append(template.substring(placeholderIndex + placeholder.length))
                                } else {
                                    append(template)
                                }
                            }

                            Text(
                                text = annotatedString,
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HudBackgroundColor)
                                    .padding(8.dp),
                            )
                        },
                    )
                }
            }

            if (helpDialogParams != null) {
                Dialog(
                    onDismissRequest = onDismissHelpDialog,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                    ) {
                        HelpDialogContent(
                            htmlContent = helpDialogParams.htmlContent,
                            actionButtonText = helpDialogParams.actionButtonText,
                            onActionButtonClick = {
                                helpDialogParams.onActionButtonClick()
                                onDismissHelpDialog()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationScreenDefaultPreview() {
    NavigatorTheme {
        Surface {
            NavigationScreen(
                isStationary = false,
                followingContact = null,
                onAddContactClick = {},
                onDismissFollowing = {},
                mapContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Map View Placeholder")
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationScreenWithHudPreview() {
    NavigatorTheme {
        Surface {
            NavigationScreen(
                isStationary = true,
                followingContact = Contact(
                    name = "Wiktor Nizio",
                    email = "wiktor.nizio@gmail.com",
                    color = android.graphics.Color.BLUE,
                ),
                onAddContactClick = {},
                onDismissFollowing = {},
                mapContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Map View Placeholder")
                    }
                },
            )
        }
    }
}
