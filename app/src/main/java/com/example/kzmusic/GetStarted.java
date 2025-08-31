package com.example.kzmusic;
//Imports
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//This class implements Get started page
public class GetStarted extends AppCompatActivity {
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    private static final String SPOTIFY_ME_URL = "https://api.spotify.com/v1/me";
    private FirebaseAuth mAuth; // Firebase Auth instance
    SessionManager sessionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_get_started);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        showSignInButton();
        //Get started button functionality
        Button btn = findViewById(R.id.get_started_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading();
                set_up_spotify_auth();
            }
        });
    }
    //These functions sets up the Spotify Sign-in/authorisation using spotify web API
    public void set_up_spotify_auth() {
        if (isNetworkAvailable()) {
            AuthorizationClient.clearCookies(getApplicationContext());
            // Spotify authorization URL
            String authUrl = AUTH_URL + "?client_id=" + CLIENT_ID +
                    "&response_type=code" +
                    "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                    "&scope=user-read-private%20user-read-email%20streaming";

            // Open the URL in a browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            startActivity(intent);
        } else {
            // Optionally, inform the user that there is no internet connection
            Toast.makeText(this, "No internet connection. Please check your network settings.", Toast.LENGTH_LONG).show();
            runOnUiThread(() -> showSignInButton());
            navigate_to_activity(MainPage.class);
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(GetStarted.this, target);
        startActivity(intent);
    }
    //This function performs this function once the activity is called and gets the auth code
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            // Extract the authorization code from the redirect URI
            String code = uri.getQueryParameter("code");
            if (code != null) {
                // Exchange the authorization code for an access token
                exchangeAuthorizationCodeForToken(code);
            } else if (uri.getQueryParameter("error") != null) {
                // Handle error from authorization
                runOnUiThread(() -> showSignInButton());
                Toast.makeText(this, "Authorization failed, please try again later.", Toast.LENGTH_SHORT).show();
                navigate_to_activity(MainPage.class);
            }
        }
    }
    // This function exchanges the auth code for the access token and expiration time
    public void exchangeAuthorizationCodeForToken(String authorizationCode) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", authorizationCode)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();

        Request request = new Request.Builder().url(TOKEN_URL).post(formBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> showSignInButton());
                runOnUiThread(() -> Toast.makeText(GetStarted.this, "Failed to exchange token, please try again.", Toast.LENGTH_SHORT).show());
                navigate_to_activity(MainPage.class); // Or back to a login screen
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String accessToken = json.getString("access_token");
                        String refreshToken = json.getString("refresh_token");
                        long expiresIn = json.getLong("expires_in");
                        // Save tokens to the central manager
                        SpotifyAuthManager.getInstance().setTokens(accessToken, refreshToken, expiresIn);
                        getSpotifyUserProfile(accessToken);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showSignInButton());
                        navigate_to_activity(MainPage.class); // Or back to a login screen
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(GetStarted.this, "Authorization failed.", Toast.LENGTH_SHORT).show());
                    runOnUiThread(() -> showSignInButton());
                }
            }
        });
    }
    /**
     * NEW METHOD
     * Fetches the user's profile from Spotify using the access token.
     */
    private void getSpotifyUserProfile(String accessToken) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SPOTIFY_ME_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(GetStarted.this, "Failed to get Spotify profile.", Toast.LENGTH_SHORT).show();
                    showSignInButton();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String spotifyEmail = json.getString("email");
                        String spotifyUsername = json.getString("display_name");
                        String spotifyId = json.getString("id"); // This will be our "password"
                        String hashedPassword = hashString(spotifyId);
                        registerUser(spotifyUsername, spotifyEmail, hashedPassword);
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(GetStarted.this, "Failed to parse profile data.", Toast.LENGTH_SHORT).show();
                            showSignInButton();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(GetStarted.this, "Could not fetch Spotify profile.", Toast.LENGTH_SHORT).show();
                        showSignInButton();
                    });
                }
            }
        });
    }
    //This function hashes a SHA-256 string for a secure password
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // This should never happen with SHA-256
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    //This function registers a new user using Firebase auth
    // This method is in your GetStarted activity or another UI class
    public void registerUser(String username, String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        UsersFirestore table = new UsersFirestore(getApplicationContext());

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        Log.d("FirebaseAuth", "User registered successfully!");
                        Toast.makeText(getApplicationContext(), "Welcome "+username+"!", Toast.LENGTH_SHORT).show();
                        table.createOrUpdateUserDocument(username, email, auth.getCurrentUser().getUid());
                        sessionManager = new SessionManager(GetStarted.this);
                        sessionManager.createLoginSession(username, email);
                        runOnUiThread(() -> showSignInButton());
                        navigate_to_activity(MainPage.class);
                    } else {
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthUserCollisionException) {
                            // This email exists, so let's try to sign the user in instead
                            Log.w("FirebaseAuth", "Email already exists. Attempting to sign in.");
                            auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(signInTask -> {
                                        if (signInTask.isSuccessful()) {
                                            Log.d("FirebaseAuth", "User signed in successfully after collision.");
                                            Toast.makeText(getApplicationContext(), "Welcome back "+username+"!", Toast.LENGTH_SHORT).show();
                                            sessionManager = new SessionManager(GetStarted.this);
                                            sessionManager.createLoginSession(username, email);
                                            runOnUiThread(() -> showSignInButton());
                                            navigate_to_activity(MainPage.class);
                                        } else {
                                            // Sign-in failed (likely an incorrect password)
                                            Log.e("FirebaseAuth", "Sign-in failed after collision.", signInTask.getException());
                                            Toast.makeText(getApplicationContext(), "Incorrect password.", Toast.LENGTH_SHORT).show();
                                            runOnUiThread(() -> showSignInButton());
                                        }
                                    });
                        } else {
                            // The error was something else (weak password, invalid email, etc.)
                            Log.e("FirebaseAuth", "Error registering user", exception);
                            Toast.makeText(getApplicationContext(), "Registration failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                            runOnUiThread(() -> showSignInButton());
                        }
                    }
                });
    }

    private void showLoading() {
        Button getStartedButton = findViewById(R.id.get_started_btn);
        ProgressBar loadingSpinner = findViewById(R.id.loading_spinner);
        getStartedButton.setVisibility(View.GONE);
        loadingSpinner.setVisibility(View.VISIBLE);
    }

    private void showSignInButton() {
        Button getStartedButton = findViewById(R.id.get_started_btn);
        ProgressBar loadingSpinner = findViewById(R.id.loading_spinner);
        getStartedButton.setVisibility(View.VISIBLE);
        loadingSpinner.setVisibility(View.INVISIBLE);
    }


}