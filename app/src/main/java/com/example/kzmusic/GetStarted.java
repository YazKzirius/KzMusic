package com.example.kzmusic;
//Imports
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.content.Context;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

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

//This class implements Get started page
public class GetStarted extends AppCompatActivity {
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    int REQUEST_CODE = 1337;
    SpotifyAppRemote mSpotifyAppRemote;
    String username;
    String email;
    String token;
    long expiration_time;
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
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(GetStarted.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
        }
        //Get started button functionality
        Button btn = findViewById(R.id.get_started_btn);
        set_up_spotify_auth();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                Toast.makeText(this, "Authorization failed, please try again later.", Toast.LENGTH_SHORT).show();
                navigate_to_activity(MainPage.class);
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
                runOnUiThread(() -> Toast.makeText(GetStarted.this, "Failed to exchange token, please try again.", Toast.LENGTH_SHORT).show());
                navigate_to_activity(MainPage.class);
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
                        // Proceed to the next activity with the token
                        navigate_to_activity(MainPage.class);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        navigate_to_activity(MainPage.class);
                    }
                }
            }
        });
    }


}