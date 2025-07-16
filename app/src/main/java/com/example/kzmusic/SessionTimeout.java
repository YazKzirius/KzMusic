package com.example.kzmusic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.spotify.sdk.android.auth.AuthorizationClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SessionTimeout extends AppCompatActivity {
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    int REQUEST_CODE = 1337;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnlinePlayerManager.getInstance().setAccess_token(null);
        OnlinePlayerManager.getInstance().setExpiration_time(0);
        OnlinePlayerManager.getInstance().setRefresh_token(null);
        //Delay dialog creation until the UI is fully ready
        new Handler(Looper.getMainLooper()).post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(SessionTimeout.this);
            builder.setTitle("Session Timed Out")
                    .setMessage("Your Spotify session has expired. Would you like to continue without Spotify or reauthorize?")
                    .setIcon(R.drawable.logo2)
                    .setCancelable(false)
                    .setPositiveButton("Continue Without Spotify", (dialog, which) -> finish())
                    .setNegativeButton("Reauthorize Spotify", (dialog, which) -> {
                        set_up_spotify_auth();
                    })

                    .show();
        });
    }
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(getApplicationContext(), target);
        startActivity(intent);
    }
    //These functions sets up the Spotify Sign-in/authorisation using spotify web API
    public void set_up_spotify_auth() {
        if (isNetworkAvailable()) {
            // Clear any stored WebView cookies to avoid persistent session reuse
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
            Toast.makeText(this, "No internet connection. Please check your network settings.", Toast.LENGTH_LONG).show();
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
    //This function performs this function once the activity is called and gets the auth code
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            // Extract the authorization code from the redirect URI
            String code = uri.getQueryParameter("code");
            if (code != null) {
                Toast.makeText(this, "Code: " + code, Toast.LENGTH_SHORT).show();
                // Exchange the authorization code for an access token
                exchangeAuthorizationCodeForToken(code);
            } else if (uri.getQueryParameter("error") != null) {
                // Handle error from authorization
                Toast.makeText(this, "Authorization failed, please try again later.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //This function exchanges the auth code for the access token and expiration time
    public void exchangeAuthorizationCodeForToken(String authorizationCode) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", authorizationCode)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to exchange token, please try again.", Toast.LENGTH_SHORT).show());

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String accessToken = json.getString("access_token");
                        String refresh = json.getString("refresh_token");
                        long expirationTime = json.getLong("expires_in");
                        OnlinePlayerManager.getInstance().setAccess_token(accessToken);
                        OnlinePlayerManager.getInstance().setRefresh_token(refresh);
                        OnlinePlayerManager.getInstance().setExpiration_time(expirationTime);
                        navigate_to_activity(MainPage.class);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}