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

package pl.org.seva.navigator.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import pl.org.seva.navigator.application.NavigatorApplication;
import pl.org.seva.navigator.R;
import pl.org.seva.navigator.databinding.ActivityGoogleSignInBinding;
import pl.org.seva.navigator.manager.DatabaseManager;

public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks {

    public static final String ACTION = "action";
    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final int SIGN_IN_REQUEST_ID = 9001;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private GoogleApiClient googleApiClient;
    private ActivityGoogleSignInBinding binding;

    private ProgressDialog progressDialog;
    private boolean performedAction;
    private boolean logoutWhenReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .build();

        firebaseAuth = FirebaseAuth.getInstance();

        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                onUserLoggedIn(user);
            }
            else {
                Log.d(TAG, "onAuthStateChanged:signed_out");
                onUserLoggedOut();
            }
        };

        if (getIntent().getStringExtra(ACTION).equals(LOGOUT)) {
            logout();
            finish();
            return;
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_google_sign_in);
        binding.signInButton.setOnClickListener(this);
    }

    private void login() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, SIGN_IN_REQUEST_ID);
    }

    private void logout() {
        finishWhenStateChanges();
        logoutWhenReady = true;
        // Firebase sign out
        firebaseAuth.signOut();
        NavigatorApplication.logout();
        googleApiClient.connect();
        finish();
    }

    private void onUserLoggedIn(FirebaseUser user) {
        DatabaseManager.getInstance().login(user);
        NavigatorApplication.login(user);
        if (performedAction) {
            finish();
        }
    }

    private void onUserLoggedOut() {
        NavigatorApplication.logout();
        if (performedAction) {
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == SIGN_IN_REQUEST_ID) {
            finishWhenStateChanges();
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
            else {
                onUserLoggedOut();
            }
        }
    }

    private void finishWhenStateChanges() {
        performedAction = true;
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        showProgressDialog();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(
                        this,
                        task -> {
                    Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                    hideProgressDialog();

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        signInFailed(task.getException());
                    }
                });
    }

    private void signInFailed(Exception ex) {
        Log.w(TAG, "signInWithCredential", ex);
        Toast.makeText(LoginActivity.this, R.string.login_authentication_failed, Toast.LENGTH_SHORT)
                .show();
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.login_loading));
            progressDialog.setIndeterminate(true);
        }

        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        if (v != binding.signInButton) {
            return;
        }
        login();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (logoutWhenReady) {
            Auth.GoogleSignInApi.signOut(googleApiClient);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //
    }
}
