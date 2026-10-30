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

package pl.org.seva.navigator.profile

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.NavigatorApplication
import pl.org.seva.navigator.main.data.fb.fbWriter
import pl.org.seva.navigator.main.extension.start
import pl.org.seva.navigator.main.ui.NavigatorTheme

fun Context.loginActivity(action: String) = start(LoginActivity::class.java) {
    putExtra(LoginActivity.ACTION, action)
}

@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: AuthStateListener
    private lateinit var googleSignInClient: GoogleSignInClient

    private val isLoading = mutableStateOf(value = false)
    private var performedAction: Boolean = false

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { activityResult ->
        finishWhenStateChanges()
        val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            signInFailed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            NavigatorTheme {
                if (isLoading.value) {
                    LoginScreen()
                }
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveTaskToBack(true)
                    finish()
                }
            },
        )

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(DEFAULT_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        firebaseAuth = FirebaseAuth.getInstance()

        authStateListener = AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
                onUserLoggedIn(user)
            } else {
                Log.d(TAG, "onAuthStateChanged:signed_out")
                onUserLoggedOut()
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        when (intent.getStringExtra(ACTION)) {
            LOGOUT -> {
                logout()
                finish()
                return
            }
            LOGIN -> {
                login()
            }
        }
    }

    private fun login() {
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun logout() {
        finishWhenStateChanges()
        firebaseAuth.signOut()
        (application as NavigatorApplication).logout()
        googleSignInClient.signOut()
        finish()
    }

    private fun onUserLoggedIn(user: FirebaseUser) {
        fbWriter login user
        (application as NavigatorApplication).login(user)
        if (performedAction) {
            finish()
        }
    }

    private fun onUserLoggedOut() {
        if (performedAction) {
            finish()
        }
    }

    public override fun onStop() {
        super.onStop()
        hideProgressDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    private fun finishWhenStateChanges() {
        performedAction = true
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + checkNotNull(acct.id))
        showProgressDialog()

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) {
                Log.d(TAG, "signInWithCredential:onComplete:" + it.isSuccessful)
                hideProgressDialog()

                if (!it.isSuccessful) {
                    signInFailed()
                }
            }
    }

    private fun signInFailed(message: String = "") {
        if (message.isNotBlank()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.login_authentication_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun showProgressDialog() {
        isLoading.value = true
    }

    private fun hideProgressDialog() {
        isLoading.value = false
    }

    companion object {

        const val DEFAULT_WEB_CLIENT_ID = "267180459782-548rckf296jcchkp8id9on17v57trcrf.apps.googleusercontent.com"

        const val ACTION = "action"
        const val LOGIN = "login"
        const val LOGOUT = "logout"

        private val TAG = LoginActivity::class.java.simpleName
    }
}
