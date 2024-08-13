package com.example.kzmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

public class MediaPlaybackService extends Service {

    private ExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private static final String CHANNEL_ID = "media_playback_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Initialize ExoPlayer
        player = PlayerManager.getInstance().current_player;

        // Initialize PlayerNotificationManager
        playerNotificationManager = new PlayerNotificationManager.Builder(getApplicationContext(), 1, CHANNEL_ID)
                .setMediaDescriptionAdapter(new DescriptionAdapter())
                .setNotificationListener(new NotificationListener())
                .build();
        playerNotificationManager.setPlayer(player);

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Media playback controls");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(this, MainPage.class); // Replace with your activity
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private class DescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {
        @Override
        public String getCurrentContentTitle(Player player) {
            return "Song Title"; // Replace with actual title
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            return createContentIntent();
        }

        @Nullable
        @Override
        public String getCurrentContentText(Player player) {
            return "Artist Name"; // Replace with actual artist name
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            return null; // Replace with album art if available
        }
    }

    private class NotificationListener implements PlayerNotificationManager.NotificationListener {
        @Override
        public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
            if (ongoing) {
                startForeground(notificationId, notification);
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        }

        @Override
        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("PLAY".equals(action)) {
                player.play();
            } else if ("PAUSE".equals(action)) {
                player.pause();
            } else if ("STOP".equals(action)) {
                player.stop();
                stopSelf();
            }
        }
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        if (playerNotificationManager != null) {
            playerNotificationManager.setPlayer(null);
            playerNotificationManager = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We do not support binding for this service
    }
}