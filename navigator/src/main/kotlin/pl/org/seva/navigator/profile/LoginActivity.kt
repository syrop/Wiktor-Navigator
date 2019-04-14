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

@file:Suppress("DEPRECATION")

package pl.org.seva.navigator.profile

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crashlytics.android.Crashlytics

import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.fabric.sdk.android.Fabric

import pl.org.seva.navigator.main.NavigatorApplication
import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.extension.start
import pl.org.seva.navigator.main.model.fb.fbWriter

fun Context.loginActivity(action: String) = start(LoginActivity::class.java) {
    putExtra(LoginActivity.ACTION, action)
}

class LoginActivity : AppCompatActivity(),
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: AuthStateListener

    private lateinit var googleApiClient: GoogleApiClient

    private var progressDialog: ProgressDialog? = null
    private var performedAction: Boolean = false
    private var logoutWhenReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(DEFAULT_WEB_CLIENT_ID)
                .requestEmail()
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .build()

        firebaseAuth = FirebaseAuth.getInstance()

        authStateListener = AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
                onUserLoggedIn(user)
            }
            else {
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
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        startActivityForResult(signInIntent, SIGN_IN_REQUEST_ID)
    }

    private fun logout() {
        finishWhenStateChanges()
        logoutWhenReady = true
        firebaseAuth.signOut()
        (application as NavigatorApplication).logout()
        googleApiClient.connect()
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

    override fun onBackPressed() {
        moveTaskToBack(true)
        finish()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == SIGN_IN_REQUEST_ID) {
            finishWhenStateChanges()
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                // Google Sign In was successful, authenticate with Firebase
                val account = result.signInAccount!!
                firebaseAuthWithGoogle(account)
            }
            else {
                signInFailed()
            }
        }
    }

    private fun finishWhenStateChanges() {
        performedAction = true
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
        showProgressDialog()

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) {
                    Log.d(TAG, "signInWithCredential:onComplete:" + it.isSuccessful)
                    hideProgressDialog()

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!it.isSuccessful) {
                        signInFailed()
                    }
                }
    }

    private fun signInFailed(message: String = "") {
        if (!message.isBlank()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, R.string.login_authentication_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage(getString(R.string.login_loading))
        progressDialog!!.isIndeterminate = true

        progressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }

    override fun onConnected(bundle: Bundle?) {
        if (logoutWhenReady) {
            Auth.GoogleSignInApi.signOut(googleApiClient)
        }
    }

    override fun onConnectionSuspended(i: Int) = Unit

    companion object {

        const val DEFAULT_WEB_CLIENT_ID = "267180459782-548rckf296jcchkp8id9on17v57trcrf.apps.googleusercontent.com"

        const val ACTION = "action"
        const val LOGIN = "login"
        const val LOGOUT = "logout"

        private val TAG = LoginActivity::class.java.simpleName

        private const val SIGN_IN_REQUEST_ID = 9001
    }
}
