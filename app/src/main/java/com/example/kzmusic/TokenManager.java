package com.example.kzmusic;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
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

public class TokenManager extends Worker {
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    int REQUEST_CODE = 1337;
    public TokenManager(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String token = getInputData().getString("refresh");
        refreshAccessToken(token);
        return Result.success();
    }

    private void refreshAccessToken(String refresh) {
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
                            OnlinePlayerManager.getInstance().setAccess_token(newAccessToken);
                            OnlinePlayerManager.getInstance().setExpiration_time(newExpirationTime);
                            // Schedule the next token refresh
                            scheduleNextTokenRefresh(newExpirationTime - 300, refresh);

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

    private void scheduleNextTokenRefresh(long refreshTime, String r) {
        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        PeriodicWorkRequest refreshWorkRequest = new PeriodicWorkRequest.Builder(
                TokenManager.class, refreshTime, TimeUnit.SECONDS)
                .setInputData(new Data.Builder().putString("refresh", r).build())
                .build();
        workManager.enqueueUniquePeriodicWork("TokenRefresh", ExistingPeriodicWorkPolicy.REPLACE, refreshWorkRequest);
    }
}