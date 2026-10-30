/*
 * Copyright (C) 2018 Wiktor Nizio
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

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.org.seva.navigator.R
import pl.org.seva.navigator.contact.ContactsRoute
import pl.org.seva.navigator.contact.SeekContactRoute
import pl.org.seva.navigator.contact.contacts
import pl.org.seva.navigator.contact.persist
import pl.org.seva.navigator.credits.CreditsRoute
import pl.org.seva.navigator.main.extension.prefs
import pl.org.seva.navigator.main.extension.toast
import pl.org.seva.navigator.main.extension.viewModel
import pl.org.seva.navigator.main.ui.NavigatorTheme
import pl.org.seva.navigator.navigation.NavigationRoute
import pl.org.seva.navigator.profile.DeleteProfileRoute
import pl.org.seva.navigator.profile.isLoggedIn
import pl.org.seva.navigator.settings.SettingsRoute

class MainActivity : AppCompatActivity() {

    private var backClickTime = 0L

    private var exitApplicationToast: Toast? = null

    private val navigatorModel by viewModel<NavigatorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NavigatorTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: NavigatorRoutes.NAVIGATION

                var isLocationPermissionGranted by remember { mutableStateOf(value = false) }
                var loggedInState by remember { mutableStateOf(value = isLoggedIn) }
                var isSearchActive by remember { mutableStateOf(value = false) }
                var helpTrigger by remember { mutableIntStateOf(value = 0) }
                var logoutTrigger by remember { mutableIntStateOf(value = 0) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            loggedInState = isLoggedIn
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                BackHandler(enabled = currentRoute == NavigatorRoutes.NAVIGATION) {
                    if ((System.currentTimeMillis() - backClickTime) >= DOUBLE_CLICK_MS) {
                        exitApplicationToast?.cancel()
                        exitApplicationToast = getString(R.string.tap_back_second_time).toast()
                        backClickTime = System.currentTimeMillis()
                    } else {
                        finish()
                    }
                }

                val title = when (currentRoute) {
                    NavigatorRoutes.CONTACTS -> stringResource(id = R.string.contacts_fragment_label)
                    NavigatorRoutes.SEEK_CONTACT -> stringResource(id = R.string.seek_contact_fragment_label)
                    NavigatorRoutes.CREDITS -> stringResource(id = R.string.credits_fragment_label)
                    NavigatorRoutes.DELETE_PROFILE -> stringResource(id = R.string.delete_user_fragment_label)
                    NavigatorRoutes.SETTINGS -> stringResource(id = R.string.settings_fragment_label)
                    else -> stringResource(id = R.string.app_name_short)
                }

                MainScreen(
                    title = title,
                    canNavigateBack = currentRoute != NavigatorRoutes.NAVIGATION,
                    onNavigateUp = {
                        isSearchActive = false
                        navController.navigateUp()
                    },
                    currentRoute = currentRoute,
                    isLocationPermissionGranted = isLocationPermissionGranted,
                    isUserLoggedIn = loggedInState,
                    onHelpClick = { helpTrigger++ },
                    onLogoutClick = {
                        logoutTrigger++
                        loggedInState = false
                    },
                    onDeleteAccountClick = {
                        navController.navigate(NavigatorRoutes.DELETE_PROFILE)
                    },
                    onSettingsClick = {
                        navController.navigate(NavigatorRoutes.SETTINGS)
                    },
                    onCreditsClick = {
                        navController.navigate(NavigatorRoutes.CREDITS)
                    },
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { active -> isSearchActive = active },
                    onSearchQuerySubmitted = { query ->
                        navigatorModel.query.value = query
                    },
                    navHostContent = {
                        NavHost(
                            navController = navController,
                            startDestination = NavigatorRoutes.NAVIGATION,
                        ) {
                            composable(NavigatorRoutes.NAVIGATION) {
                                NavigationRoute(
                                    navigatorModel = navigatorModel,
                                    onNavigateToContacts = {
                                        navController.navigate(NavigatorRoutes.CONTACTS)
                                    },
                                    isLocationPermissionGranted = isLocationPermissionGranted,
                                    onLocationPermissionChanged = { granted ->
                                        isLocationPermissionGranted = granted
                                    },
                                    helpTrigger = helpTrigger,
                                    logoutTrigger = logoutTrigger,
                                )
                            }
                            composable(NavigatorRoutes.CONTACTS) {
                                ContactsRoute(
                                    navigatorModel = navigatorModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToSeekContact = {
                                        navController.navigate(NavigatorRoutes.SEEK_CONTACT)
                                    },
                                )
                            }
                            composable(NavigatorRoutes.SEEK_CONTACT) {
                                SeekContactRoute(
                                    navigatorModel = navigatorModel,
                                    isSearchActive = isSearchActive,
                                    onNavigateBack = {
                                        isSearchActive = false
                                        navController.popBackStack()
                                    },
                                )
                            }
                            composable(NavigatorRoutes.CREDITS) {
                                CreditsRoute()
                            }
                            composable(NavigatorRoutes.DELETE_PROFILE) {
                                DeleteProfileRoute(
                                    navigatorModel = navigatorModel,
                                    onNavigateBack = { navController.popBackStack() },
                                )
                            }
                            composable(NavigatorRoutes.SETTINGS) {
                                SettingsRoute()
                            }
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(CONTACT_EMAIL_EXTRA)?.apply {
            val contact = contacts[this]
            navigatorModel.contact.value = contact
            contact persist prefs
        }
        if (Intent.ACTION_SEARCH == intent.action) {
            navigatorModel.query.value = intent.getStringExtra(SearchManager.QUERY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as NavigatorApplication).stopService()
    }

    companion object {
        /** Length of time that will be taken for a double click.  */
        private const val DOUBLE_CLICK_MS: Long = 1000

        const val CONTACT_EMAIL_EXTRA = "contact_email"
    }
}
