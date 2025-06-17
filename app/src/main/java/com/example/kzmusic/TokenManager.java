package com.example.kzmusic;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TokenManager {
    private static TokenManager instance;
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    Context context;
    int REQUEST_CODE = 1337;
    private TokenManager(Context context) {
        this.context = context.getApplicationContext(); // ✅ Store global context
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
        return instance;
    }

    public void refreshAccessToken(String refresh, TokenCallback callback) {
        if (refresh != null) {
            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refresh)
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
                    // Handle failure
                    ;
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String newAccessToken = json.getString("access_token");
                            long newExpirationTime = json.getLong("expires_in");
                            if (json.has("refresh_token")) {
                                OnlinePlayerManager.getInstance().setRefresh_token(json.getString("refresh_token"));
                            }

                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onSuccess(newAccessToken)
                            );

                            OnlinePlayerManager.getInstance().setAccess_token(newAccessToken);
                            OnlinePlayerManager.getInstance().setRefresh_token(refresh);
                            OnlinePlayerManager.getInstance().setExpiration_time(newExpirationTime);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            // Handle JSON parsing error
                            ;
                        }
                    } else {
                        // Handle unsuccessful response
                        ;
                    }
                }
            });
        } else {
            // Handle null refresh token
        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}