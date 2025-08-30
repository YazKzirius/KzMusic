package com.example.kzmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;


public class TokenRefreshService extends Service {
    private static final String CHANNEL_ID = "TokenRefreshChannel";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tokenRefreshRunnable;

    // Refresh every 55 minutes (55 * 60 * 1000 milliseconds)
    private static final long REFRESH_INTERVAL = 3300000;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());

        tokenRefreshRunnable = () -> {
            Log.d("TokenRefreshService", "🔄 Performing scheduled token refresh...");
            SpotifyAuthManager.getInstance().refreshAccessToken();
            // Schedule the next run
            handler.postDelayed(tokenRefreshRunnable, REFRESH_INTERVAL);
        };

        // Start the periodic refresh
        handler.post(tokenRefreshRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Ensures service auto-restarts
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
        if (manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Token Refresh Service", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Service")
                .setContentText("Keeping your Spotify session active.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}