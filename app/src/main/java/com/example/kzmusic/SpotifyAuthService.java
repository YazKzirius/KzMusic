package com.example.kzmusic;

//Imports
import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class SpotifyAuthService {
    private static final String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    private static final String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    private static final String AUTH_URL = "https://accounts.spotify.com/api/token";

    private String accessToken;
    private long tokenExpirationTime;

    public void getAccessToken(Callback<String> callback) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            callback.onSuccess(accessToken);
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url(AUTH_URL)
                .post(body)
                .header("Authorization", credentials)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    accessToken = jsonObject.get("access_token").getAsString();
                    int expiresIn = jsonObject.get("expires_in").getAsInt();
                    tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000);
                    callback.onSuccess(accessToken);
                } else {
                    callback.onFailure(new IOException("Unexpected code " + response));
                }
            }
        });
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(Throwable t);
    }
}
