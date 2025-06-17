package com.example.kzmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;


public class TokenRefreshService extends Service {
    private static final String CHANNEL_ID = "TokenRefreshChannel";
    private Handler handler = new Handler(Looper.getMainLooper());
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
            @Override
            public void run() {
                Log.d("TokenRefreshService", "🔄 Refreshing access token...");
                long expiration = OnlinePlayerManager.getInstance().getExpiration_time();

                TokenManager.getInstance(getApplicationContext()).refreshAccessToken(
                        OnlinePlayerManager.getInstance().getRefresh_token(),
                        new TokenCallback() {
                            @Override
                            public void onSuccess(String newAccessToken) {
                                Log.d("TokenRefreshService", "✅ New Access Token: " + newAccessToken);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("TokenRefreshService", "❌ Token refresh failed: " + e.getMessage());
                            }
                        }
                );
                handler.postDelayed(this, 5000); // 🔄 Refresh every 5 seconds
            }
        };

        handler.post(tokenRefreshRunnable); // Start refresh loop
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

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Spotify Token Service")
                .setContentText("Keeping your access token fresh.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
