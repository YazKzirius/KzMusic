package com.example.kzmusic;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyAuthManager {

    private static volatile SpotifyAuthManager instance;
    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- Spotify Credentials (Keep these secure!) ---
    private static final String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    private static final String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    private String accessToken;
    private String refreshToken;
    private long expiresAt; // The exact timestamp when the token expires

    private boolean isRefreshing = false;
    private final List<TokenCallback> pendingCallbacks = new ArrayList<>();

    // Private constructor for Singleton pattern
    private SpotifyAuthManager() {}

    public static SpotifyAuthManager getInstance() {
        if (instance == null) {
            synchronized (SpotifyAuthManager.class) {
                if (instance == null) {
                    instance = new SpotifyAuthManager();
                }
            }
        }
        return instance;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setTokens(String accessToken, String refreshToken, long expiresInSeconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        // Calculate the absolute expiration time in milliseconds
        this.expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
        Log.d("SpotifyAuthManager", "Tokens set. New token expires at: " + new java.util.Date(this.expiresAt));
    }

    /**
     * The main method for the UI to get a valid token.
     * It handles checking, refreshing, and queuing requests.
     */
    public void getValidAccessToken(TokenCallback callback) {
        if (isTokenValid()) {
            callback.onTokenReceived(accessToken);
            return;
        }

        // Add callback to the queue and trigger a refresh if one isn't already running
        synchronized (this) {
            pendingCallbacks.add(callback);
            if (!isRefreshing) {
                isRefreshing = true;
                refreshAccessTokenInternal();
            }
        }
    }

    /**
     * Proactively refreshes the token. Called by the background service.
     */
    public void refreshAccessToken() {
        if (isRefreshing) return; // Don't start a new refresh if one is in progress
        Log.d("SpotifyAuthManager", "Proactive token refresh triggered by service.");
        isRefreshing = true;
        refreshAccessTokenInternal();
    }


    private void refreshAccessTokenInternal() {
        if (refreshToken == null) {
            Log.e("SpotifyAuthManager", "Refresh token is null. Cannot refresh.");
            notifyCallbacks(null);
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();

        Request request = new Request.Builder().url(TOKEN_URL).post(formBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SpotifyAuthManager", "Token refresh failed.", e);
                notifyCallbacks(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String newAccessToken = json.getString("access_token");
                        long expiresIn = json.getLong("expires_in");
                        // Sometimes Spotify provides a new refresh token
                        if (json.has("refresh_token")) {
                            refreshToken = json.getString("refresh_token");
                        }
                        setTokens(newAccessToken, refreshToken, expiresIn);
                        Log.d("SpotifyAuthManager", "✅ Token refreshed successfully.");
                        Log.d("SpotifyAuthManager", "New Access Token: "+newAccessToken);
                        notifyCallbacks(newAccessToken);
                    } catch (JSONException e) {
                        Log.e("SpotifyAuthManager", "Failed to parse refresh response.", e);
                        notifyCallbacks(null);
                    }
                } else {
                    Log.e("SpotifyAuthManager", "Refresh failed with code: " + response.code());
                    notifyCallbacks(null); // Critical failure, token is invalid
                }
            }
        });
    }

    private void notifyCallbacks(String token) {
        synchronized (this) {
            for (TokenCallback cb : pendingCallbacks) {
                mainHandler.post(() -> {
                    if (token != null) {
                        cb.onTokenReceived(token);
                    } else {
                        cb.onError();
                    }
                });
            }
            pendingCallbacks.clear();
            isRefreshing = false;
        }
    }

    private boolean isTokenValid() {
        if (accessToken == null || refreshToken == null) {
            return false;
        }
        // Check if the token expires in the next 60 seconds to be safe
        return (expiresAt - System.currentTimeMillis()) > 60000;
    }

    public void logout(Context context) {
        this.accessToken = null;
        this.refreshToken = null;
        this.expiresAt = 0;
        // Optionally clear shared preferences here
        // Navigate to login screen
        Intent intent = new Intent(context, GetStarted.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

// --- Helper Interface ---
interface TokenCallback {
    void onTokenReceived(String accessToken);
    void onError();
}
