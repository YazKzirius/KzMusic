package com.example.kzmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

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


public class TokenRefreshService extends Service {
    private static final String CHANNEL_ID = "TokenRefreshChannel";
    private Handler handler = new Handler(Looper.getMainLooper());
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    int REQUEST_CODE = 1337;
    private Runnable tokenRefreshRunnable;
    public class LocalBinder extends Binder {
        public TokenRefreshService getService() {
            return TokenRefreshService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());

        tokenRefreshRunnable = new Runnable() {
            private boolean firstRun = true; // ✅ Track first execution

            @Override
            public void run() {
                Log.d("TokenRefreshService", "🔄 Checking token expiration...");
                if (firstRun) {
                    firstRun = false; // ✅ Mark first execution as completed
                    Log.d("TokenRefreshService", "⚡ First execution—skipping session timeout check.");
                } else {
                    Log.d("TokenRefreshService", "🚨 Session Timed Out!");
                    if (OnlinePlayerManager.getInstance().getRefresh_token() != null) {
                        refreshSpotifyToken(OnlinePlayerManager.getInstance().getRefresh_token());
                    }
                }
                Log.d("TokenRefreshService", "⏳ Next expiration check in 5 seconds.");
                if (OnlinePlayerManager.getInstance().getExpiration_time() != 0) {
                    handler.postDelayed(this, (OnlinePlayerManager.getInstance().getExpiration_time()-600)*1000); // ✅ Runs indefinitely every 5 seconds
                } else {
                    ;
                }

            }
        };
        handler.post(tokenRefreshRunnable); // ✅ Start refresh loop
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // ✅ Ensures service auto-restarts after app closure
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tokenRefreshRunnable);
        Log.d("TokenRefreshService", "❌ Token refresh service stopped!");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Token Refresh Service", NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(channel);
    }
    public static void stopTokenService(Context context) {
        Intent stopIntent = new Intent(context, TokenRefreshService.class);
        context.stopService(stopIntent); // ✅ Stops the service from anywhere
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Spotify Token Service")
                .setContentText("Keeping your access token fresh.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    //This function resets the access token
    public void refreshSpotifyToken(String refreshToken) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
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
                Log.e("SpotifyAuth", "Token refresh failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        long expirationTime = json.getLong("expires_in");
                        String accessToken = json.getString("access_token");
                        // Some responses may include a new refresh token — handle that too
                        OnlinePlayerManager.getInstance().setAccess_token(accessToken);
                        OnlinePlayerManager.getInstance().setExpiration_time(expirationTime);
                        Log.d("SpotifyAuth", "✅ Token refreshed successfully: "+accessToken);
                        Log.d("SpotifyAuth", "✅ Expiration:  "+expirationTime);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("SpotifyAuth", "Failed to parse refresh response");
                    }
                } else {
                    Log.e("SpotifyAuth", "Refresh failed: " + response.code());
                }
            }
        });
    }
}