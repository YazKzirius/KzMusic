package com.example.kzmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
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
            private boolean firstRun = true; // ✅ Track first execution

            @Override
            public void run() {
                Log.d("TokenRefreshService", "🔄 Checking token expiration...");
                long expirationTime = OnlinePlayerManager.getInstance().getExpiration_time();
                if (firstRun) {
                    firstRun = false; // ✅ Mark first execution as completed
                    Log.d("TokenRefreshService", "⚡ First execution—skipping session timeout check.");
                } else {
                    OnlinePlayerManager.getInstance().setAccess_token(null);
                    OnlinePlayerManager.getInstance().setExpiration_time(0);
                    OnlinePlayerManager.getInstance().setRefresh_token(null);
                    Log.d("TokenRefreshService", "🚨 Session Timed Out!");
                    Intent intent = new Intent(getApplicationContext(), SessionTimeout.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent); // ✅ Show popup screen
                }
                Log.d("TokenRefreshService", "⏳ Next expiration check in 5 seconds.");
                if (expirationTime != 0) {
                    handler.postDelayed(this, (expirationTime-300)*1000); // ✅ Runs indefinitely every 5 seconds
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
}