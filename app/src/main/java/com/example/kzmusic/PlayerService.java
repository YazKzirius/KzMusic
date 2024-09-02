package com.example.kzmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import java.io.File;
import java.util.Random;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;

public class PlayerService extends Service {

    String CHANNEL_ID = "player_channel_id";
    int NOTIFICATION_ID = 1;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private EnvironmentalReverb reverb;
    int session_id;
    ExoPlayer player;
    SharedViewModel sharedViewModel;
    private static final String ACTION_SKIP_NEXT = "com.example.ACTION_SKIP_NEXT";
    private static final String ACTION_SKIP_PREVIOUS = "com.example.ACTION_SKIP_PREVIOUS";
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Stopping all notification sessions for single session management
        if (PlayerManager.getInstance().get_size() > 0) {
            PlayerManager.getInstance().StopAllSessions();
        }
        //Updating channel ID settings
        SongQueue.getInstance().update_id();
        NOTIFICATION_ID = SongQueue.getInstance().NOTIFICATION_ID;
        CHANNEL_ID += NOTIFICATION_ID;
        sharedViewModel = SharedViewModelProvider.getViewModel(this);
        initializeMediaSession();
        createNotificationChannel();
    }
    private void handleSkip() {
        // Trigger the skip event in the ViewModel
        if (sharedViewModel != null) {
            sharedViewModel.triggerSkipEvent();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle incoming intents (e.g., play, pause, stop)
        return START_NOT_STICKY;
    }

    //This function checks if a string is only digits
    public boolean isOnlyDigits(String str) {
        str = str.replaceAll(" ", "");
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    //This function formats song title, removing unnecessary data
    public String format_title(String title) {
        //Removing unnecessary data
        title = title.replace("[SPOTIFY-DOWNLOADER.COM] ", "").replace(".mp3", "").replaceAll("_", " ").replaceAll("  ", " ").replace(".flac", "").replace(".wav", "");
        //Checking if prefix is a number
        String prefix = title.charAt(0) + "" + title.charAt(1) + "" + title.charAt(2);
        //Checking if prefix is at the start and if it occurs again
        if (isOnlyDigits(prefix) && title.indexOf(prefix) == 0 && title.indexOf(prefix, 2) == -1) {
            //Removing prefix
            title = title.replaceFirst(prefix, "");
        } else {
            ;
        }
        return title;
    }
    //This function opens a new song overlay
    public void open_new_overlay(MusicFile file, int position) {
        //Adding song to queue
        SongQueue.getInstance().addSong(file);
        SongQueue.getInstance().setPosition(position);
        Fragment media_page = new MediaOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getApplicationContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
    }
    //This function initializes media session for notification
    public void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(this, "ExoPlayerMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, (float) 1.0);
        Random rand = new Random();
        mediaSession.setPlaybackState(stateBuilder.build());
        PlayerManager.getInstance().addSession(mediaSession);
        showNotification(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                PlayerManager.getInstance().current_player.play();
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                showNotification(stateBuilder.build());
            }

            @Override
            public void onPause() {
                super.onPause();
                PlayerManager.getInstance().current_player.pause();
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                showNotification(stateBuilder.build());
            }
            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                int pos;
                if (SongQueue.getInstance().current_position == SongQueue.getInstance().song_list.size() - 1 &&
                        SongQueue.getInstance().shuffle_on != true) {
                    ;
                } else {
                    if (SongQueue.getInstance().shuffle_on != true) {
                        pos = SongQueue.getInstance().current_position + 1;
                    } else {
                        pos = rand.nextInt(SongQueue.getInstance().song_list.size());
                    }
                    MusicFile song = SongQueue.getInstance().song_list.get(pos);
                    //Error handling
                    try {
                        open_new_overlay(song, pos);
                    } catch (Exception e) {
                        SongQueue.getInstance().addSong(song);
                        SongQueue.getInstance().setPosition(pos);
                        updateNotification(song);
                        handleSkip();
                    }
                }
            }
            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                int pos;
                if (SongQueue.getInstance().current_position == 0 &&
                        SongQueue.getInstance().shuffle_on != true) {
                    ;
                } else {
                    if (SongQueue.getInstance().shuffle_on != true) {
                        pos = SongQueue.getInstance().current_position - 1;
                    } else {
                        pos = rand.nextInt(SongQueue.getInstance().song_list.size());
                    }
                    MusicFile song = SongQueue.getInstance().song_list.get(pos);
                    //Error handling
                    try {
                        open_new_overlay(song, pos);
                    } catch (Exception e) {
                        SongQueue.getInstance().addSong(song);
                        SongQueue.getInstance().setPosition(pos);
                        updateNotification(song);
                        handleSkip();
                    }
                }
            }
        });

        mediaSession.setActive(true);
    }

    public void updatePlaybackState(int state) {
        stateBuilder.setState(state, 0, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media playback",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Media playback controls");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNotification(PlaybackStateCompat state) {
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(SongQueue.getInstance().current_song.getAlbumId()));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.library)
                .setContentTitle(format_title(SongQueue.getInstance().current_song.getName()))
                .setContentText(SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", "))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0));
        Glide.with(this).asBitmap().load(album_uri).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                builder.setLargeIcon(resource);
                if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    builder.addAction(new NotificationCompat.Action(
                            R.drawable.ic_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            getApplicationContext(), PlaybackStateCompat.ACTION_PAUSE)));
                } else {
                    builder.addAction(new NotificationCompat.Action(
                            R.drawable.ic_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            getApplicationContext(), PlaybackStateCompat.ACTION_PLAY)));
                }

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                startForeground(NOTIFICATION_ID, builder.build());
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
                if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    builder.addAction(new NotificationCompat.Action(
                            R.drawable.ic_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            getApplicationContext(), PlaybackStateCompat.ACTION_PAUSE)));
                } else {
                    builder.addAction(new NotificationCompat.Action(
                            R.drawable.ic_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(
                            getApplicationContext(), PlaybackStateCompat.ACTION_PLAY)));
                }

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                startForeground(NOTIFICATION_ID, builder.build());
            }
        });
    }
    //This function updates the current notification view holder when a song is skipped
    public void updateNotification(MusicFile musicFile) {
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(musicFile.getAlbumId()));
        //Updating current notification with new details and meta data
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.library)
                .setContentTitle(format_title(musicFile.getName()))
                .setContentText(musicFile.getArtist().replaceAll("/", ", "))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0));
        Glide.with(this).asBitmap().load(album_uri).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                builder.setLargeIcon(resource);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
    }
}
