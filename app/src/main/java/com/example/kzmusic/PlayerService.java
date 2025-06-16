package com.example.kzmusic;

//Imports
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.analytics.PlaybackStats;
import com.google.android.exoplayer2.analytics.PlaybackStatsListener;


//Song player service / Media notification class
public class PlayerService extends Service {

    String CHANNEL_ID = "player_channel_id";
    int NOTIFICATION_ID = 1;
    private MediaSessionCompat mediaSession;
    PlaybackStateCompat.Builder stateBuilder;
    private EnvironmentalReverb reverb;
    int session_id;
    ExoPlayer player;
    SharedViewModel sharedViewModel;
    private static final String ACTION_SKIP_NEXT = "com.example.ACTION_SKIP_NEXT";
    private static final String ACTION_SKIP_PREVIOUS = "com.example.ACTION_SKIP_PREVIOUS";
    private final IBinder binder = new LocalBinder();
    private long last_position;

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Stopping all notification sessions for single session management
        if (OfflinePlayerManager.getInstance().get_size() > 0) {
            OfflinePlayerManager.getInstance().StopAllSessions();
        }
        //Updating channel ID settings
        SongQueue.getInstance().update_id();
        //Playing the song
        NOTIFICATION_ID = SongQueue.getInstance().NOTIFICATION_ID;
        CHANNEL_ID += NOTIFICATION_ID;
        createNotificationChannel();
        initializeMediaSession();
        sharedViewModel = SharedViewModelProvider.getViewModel(this);
    }
    //This function plays the specified music file
    public void playMusic(MusicFile musicFile) {
        //Playing resuming song at previous duration if the same song as last
        if (SongQueue.getInstance().get_size() > 1) {
            int index = SongQueue.getInstance().pointer - 1;
            //Getting current and previous song names
            String s1 = SongQueue.getInstance().get_specified(index).getName();
            String s2 = SongQueue.getInstance().get_specified(index - 1).getName();
            if (s1.equals(s2)) {
                //Resuming at left point
                //Use previous player
                player = OfflinePlayerManager.getInstance().current_player;
            } else {
                OfflinePlayerManager.getInstance().stopAllPlayers();
                player = new ExoPlayer.Builder(getApplicationContext()).build();
                Uri uri = Uri.fromFile(new File(musicFile.getPath()));
                MediaItem mediaItem = MediaItem.fromUri(uri);
                player.setMediaItem(mediaItem);
                add_song(musicFile);

            }
        } else {
            player = new ExoPlayer.Builder(getApplicationContext()).build();
            Uri uri = Uri.fromFile(new File(musicFile.getPath()));
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            add_song(musicFile);
        }
        //Initializing song properties
        session_id = player.getAudioSessionId();
        //Adds player to Player session manager
        OfflinePlayerManager.getInstance().addPlayer(player);
        OfflinePlayerManager.getInstance().setCurrent_player(player);
        SongQueue.getInstance().setAudio_session_id(session_id);
        enable_endless_stream();
        //Applying audio effects
        apply_audio_effect();
        player.prepare();
        player.play();

    }
    //This function adds new song to firestore collection
    public void add_song(MusicFile musicFile) {
        String current_display_title = musicFile.getName();
        String artist = musicFile.getArtist().replaceAll("/", ", ");
        current_display_title = current_display_title.replaceAll("by "+artist, "").replaceAll(
                "- "+artist, "");
        if (isOnlyDigits(current_display_title)) {
            current_display_title = current_display_title +" by "+ artist;
        } else {
            current_display_title = format_title(current_display_title) +" by "+ artist;
        }

        final String finalDisplayTitleForLambda = current_display_title; // This is effectively final

        //Adding song to database
        SessionManager sessionManager = new SessionManager(getApplicationContext());
        String email = sessionManager.getEmail();
        SongsFirestore table = new SongsFirestore(getApplicationContext());
        table.db.collection("Users").whereEqualTo("EMAIL", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String user_id = querySnapshot.getDocuments().get(0).getId();
                        // 🔥 Check if the same song exists for the user
                        table.db.collection("Songs")
                                .whereEqualTo("TITLE", finalDisplayTitleForLambda)
                                .whereEqualTo("USER_ID", user_id) // Ensure user does not have this song already
                                .get()
                                .addOnSuccessListener(songSnapshot -> {
                                    if (songSnapshot.isEmpty()) {
                                        table.add_new_song(email, finalDisplayTitleForLambda, musicFile.getArtist());
                                    } else {
                                        table.updateTimesPlayed(email, finalDisplayTitleForLambda);
                                    }
                                });
                    }
                });
    }
    //This function assigns audio effects to the exoplayer like speed/reverb
    public void apply_audio_effect() {
        //Initialising reverb settings
        SongQueue.getInstance().initialize_reverb(session_id);
        reverb = SongQueue.getInstance().reverb;
        //Setting playback speed properties
        player.setPlaybackParameters(new PlaybackParameters(SongQueue.getInstance().speed, SongQueue.getInstance().pitch));
        //Setting reverberation properties
        setReverbPreset(SongQueue.getInstance().reverb_level);
    }
    //This function sets reverb level based on seekbar progress level
    public void setReverbPreset(int progress) {
        //Computing reverberation parameters based of reverb level data proportionality
        try {
            int room_level = -2000 + (progress + 1000);
            double decay_level = 10000;
            reverb.setReverbLevel((short) progress);
            reverb.setDecayTime((int) decay_level);
            reverb.setRoomLevel((short) room_level);
            reverb.setEnabled(true);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Something went wrong with audio effects", Toast.LENGTH_SHORT).show();
        }
    }
    public void handleSkip() {
        // Trigger the skip event in the ViewModel
        if (sharedViewModel != null) {
            sharedViewModel.triggerSkipEvent();
        }
    }
    public void handleEnd() {
        // Trigger the End event in the ViewModel
        if (sharedViewModel != null) {
            sharedViewModel.triggerEndEvent();
        }
    }
    public void handlePause() {
        // Trigger the skip event in the ViewModel
        if (sharedViewModel != null) {
            sharedViewModel.triggerPauseEvent();
        }
    }
    public void handlePlay() {
        // Trigger the skip event in the ViewModel
        if (sharedViewModel != null) {
            sharedViewModel.triggerPlayEvent();
        }
    }
    public void handleUpdate() {
        // Trigger the skip event in the ViewModel
        if (sharedViewModel != null) {
            sharedViewModel.triggerUpdateEvent();
        }
    }
    public void handleStop() {
        // Trigger the skip event in the ViewModel
        if (sharedViewModel != null) {
            sharedViewModel.triggerStopEvent();
        }
    }
    public void setViewModel(SharedViewModel viewModel) {
        this.sharedViewModel = viewModel;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle incoming intents (e.g., play, pause, stop)
        return START_NOT_STICKY;
    }
    // This function formats song title, removing unnecessary data and watermarks
    public String format_title(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "";
        }

        String originalTitleBeforeNumericCheck = title; // Store original for potential revert
        String workingTitle = title;

        // 1. More aggressive generic watermark removal
        workingTitle = workingTitle.replaceAll("\\[[^\\]]*\\]", "");
        workingTitle = workingTitle.replaceAll("\\((?i)(official|lyric|video|audio|hd|hq|explicit|remastered|live|original|mix|feat|ft\\.)[^)]*\\)", "");

        // 2. Remove common file extensions
        workingTitle = workingTitle.replaceAll("(?i)\\.(mp3|flac|wav|m4a|ogg|aac|wma)$", "");

        // --- Store the state of workingTitle before attempting to strip leading numbers ---
        // This is what we might revert to if stripping numbers leaves only other numbers.
        String titleAfterWatermarksAndExtensions = workingTitle.trim();
        workingTitle = titleAfterWatermarksAndExtensions;

        // 4. Replace underscores and multiple spaces with a single space
        workingTitle = workingTitle.replaceAll("[_]+", " ");
        workingTitle = workingTitle.replaceAll("\\s+", " ");

        // 5. Trim leading/trailing whitespace
        workingTitle = workingTitle.trim();


        // 6. FINAL CHECKS
        // If the result of all cleaning is ONLY digits, and the original wasn't just those digits (meaning watermarks/extensions were removed)
        // then it's likely the "title" part was just numbers. In this case, we prefer the version before number stripping,
        // or even the original if the number-only version is too bare.
        if (isOnlyDigits(workingTitle) && !workingTitle.isEmpty()) {
            // If 'titleAfterLeadingNumberRemoval' (which is 'workingTitle' before this final digit check)
            // is also all digits and is the same as the current 'workingTitle',
            // it means the leading number removal didn't change anything, or it removed a prefix and left digits.
            // Consider 'titleAfterWatermarksAndExtensions'. If it wasn't just digits, prefer that.
            if (!isOnlyDigits(titleAfterWatermarksAndExtensions) && !titleAfterWatermarksAndExtensions.isEmpty()) {
                // If the version before number stripping had letters, it was better.
                return titleAfterWatermarksAndExtensions;
            } else {
                // If even after watermarks/extensions it was just numbers, or became just numbers,
                // and the current workingTitle is also just numbers,
                // it implies the original might have been "numbers.mp3" or "[watermark] numbers".
                // In this specific case, returning the 'workingTitle' (which is just numbers) is acceptable
                // as per the request "if a song name is just numbers after ... handling, return the string [of numbers]".
                // However, if the original title had more context, we might prefer 'originalTitleBeforeNumericCheck'.
                // This logic becomes a bit about preference. For now, let's stick to returning the numbers if that's what's left.
            }
        }


        // If after all operations, the title becomes empty (e.g., it was just "[SPOTIFY-DOWNLOADER.COM].mp3")
        // return the original title (or a placeholder) if the original had meaningful content.
        if (workingTitle.isEmpty() && !originalTitleBeforeNumericCheck.trim().isEmpty()) {
            // Check if the original, after basic cleaning (watermarks/extensions) was also empty or just noise
            String tempCleanedOriginal = originalTitleBeforeNumericCheck.replaceAll("\\[[^\\]]*\\]", "")
                    .replaceAll("\\((?i)(official|lyric|video|audio|hd|hq|explicit|remastered|live|original|mix|feat|ft\\.)[^)]*\\)", "")
                    .replaceAll("(?i)\\.(mp3|flac|wav|m4a|ogg|aac|wma)$", "").trim();
            if(tempCleanedOriginal.isEmpty() || tempCleanedOriginal.matches("^[\\s.-]*$") || isOnlyDigits(tempCleanedOriginal)) {
                // If the original itself was essentially just a watermark/number/extension, returning an empty string might be fine
                // or return the numeric part if that's all that's left of the original.
                if(isOnlyDigits(tempCleanedOriginal) && !tempCleanedOriginal.isEmpty()) return tempCleanedOriginal;
                // else if original was just noise, and working title is empty, original request for this state is not to return original title
            } else {
                return originalTitleBeforeNumericCheck; // Prefer fuller original if cleaning nuked a valid title
            }
        }
        Pattern leadingNoisePattern = Pattern.compile("^[^a-zA-Z0-9]*\\d{1,4}[\\s.-]*");
        Matcher noiseMatcher = leadingNoisePattern.matcher(workingTitle);
        if (noiseMatcher.find()) {
            String afterNoise = workingTitle.substring(noiseMatcher.end());
            // Only remove if what follows isn't empty or just noise itself
            if (!afterNoise.trim().isEmpty() && afterNoise.matches(".*[a-zA-Z].*")) { // Check if there's at least one letter after
                workingTitle = afterNoise;
            }
        }

        // 4. Replace underscores and multiple spaces with a single space
        workingTitle = workingTitle.replaceAll("[_]+", " ");
        workingTitle = workingTitle.replaceAll("\\s+", " ");

        // 5. Trim leading/trailing whitespace that might have been introduced or was already there.
        workingTitle = workingTitle.trim();

        // 6. Handle cases where the title might become just a separator after cleaning
        if (workingTitle.matches("^[\\s.-]*$")) { // If only spaces, dots, hyphens remain
            return title; // Revert to original title if cleaning results in effectively nothing meaningful
        }


        // If after all operations, the title becomes empty (e.g., it was just "[SPOTIFY-DOWNLOADER.COM].mp3")
        // return the original title or a placeholder, rather than an empty string if the original had content.
        if (workingTitle.isEmpty() && !title.trim().isEmpty()) {
            return title; // Or a specific placeholder like "Unknown Title"
        }
        return workingTitle;
    }

    // Helper function to check if a string contains only digits
    private boolean isOnlyDigits(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        String trimmedStr = str.trim(); // Trim spaces before checking
        if (trimmedStr.isEmpty()) {
            return false;
        }
        for (char c : trimmedStr.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
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
        OfflinePlayerManager.getInstance().addSession(mediaSession);
        showNotification(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                OfflinePlayerManager.getInstance().current_player.play();
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                showNotification(stateBuilder.build());
                handlePlay();
            }

            @Override
            public void onPause() {
                super.onPause();
                OfflinePlayerManager.getInstance().current_player.pause();
                //Update database duration
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                showNotification(stateBuilder.build());
                handlePause();
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
                    SongQueue.getInstance().addSong(song);
                    SongQueue.getInstance().setPosition(pos);
                    updateNotification(song);
                    playMusic(song);
                    handleSkip();
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
                    SongQueue.getInstance().addSong(song);
                    SongQueue.getInstance().setPosition(pos);
                    playMusic(song);
                    updateNotification(song);
                    handleSkip();
                }
            }
        });

        mediaSession.setActive(true);
    }

    public void updatePlaybackState(int state) {
        //Checking if state is on repeat and implementing functionality
        if (state == PlaybackStateCompat.REPEAT_MODE_ONE) {
            OfflinePlayerManager.getInstance().current_player.setRepeatMode(Player.REPEAT_MODE_ONE);
            stateBuilder.setState(stateBuilder.build().getState(), 0, SongQueue.getInstance().speed);
        } else {
            stateBuilder.setState(state, 0, SongQueue.getInstance().speed);
        }
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
        String display_title = SongQueue.getInstance().current_song.getName();
        String artist = SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", ");
        display_title = display_title.replaceAll("by "+artist, "").replaceAll(
                "- "+artist, "");
        if (isOnlyDigits(display_title)) {
            display_title = display_title +" by "+ artist;
        } else {
            display_title = format_title(display_title) +" by "+ artist;
        }
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(SongQueue.getInstance().current_song.getAlbumId()));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.library)
                .setContentTitle(display_title)
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
                ;
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
        String display_title = musicFile.getName();
        String artist = musicFile.getArtist().replaceAll("/", ", ");
        display_title = display_title.replaceAll("by "+artist, "").replaceAll(
                "- "+artist, "");
        if (isOnlyDigits(display_title)) {
            display_title = display_title +" by "+ artist;
        } else {
            display_title = format_title(display_title) +" by "+ artist;
        }
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(musicFile.getAlbumId()));
        //Updating current notification with new details and meta data
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.library)
                .setContentTitle(display_title)
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
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                startForeground(NOTIFICATION_ID, builder.build());
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                startForeground(NOTIFICATION_ID, builder.build());
            }
        });
    }
    //This function simulates end of song
    public void simulateEndOfSong() {
        // Call this method to simulate the end of a song
        OfflinePlayerManager.getInstance().current_player.stop();
        OfflinePlayerManager.getInstance().current_player.seekTo(OfflinePlayerManager.getInstance().current_player.getDuration());
        OfflinePlayerManager.getInstance().current_player.release();
    }

    //This function enables end of song skipping for endless streaming
    public void enable_endless_stream() {
        Random rand = new Random();
        //Adding player listener
        OfflinePlayerManager.getInstance().current_player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    handleUpdate();
                } else {
                    handleStop();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    simulateEndOfSong();
                    int pos;
                    MusicFile song;
                    if (SongQueue.getInstance().current_position == SongQueue.getInstance().song_list.size() - 1 &&
                            SongQueue.getInstance().shuffle_on != true) {
                        pos = 0;
                        song = SongQueue.getInstance().song_list.get(pos);
                    } else {
                        if (SongQueue.getInstance().shuffle_on != true) {
                            pos = SongQueue.getInstance().current_position + 1;
                        } else {
                            pos = rand.nextInt(SongQueue.getInstance().song_list.size());
                        }
                        //Checking if next song is the same song and handling exception accordingly
                        song = SongQueue.getInstance().song_list.get(pos);
                        if (song.getName().equals(SongQueue.getInstance().current_song.getName())) {
                            if (SongQueue.getInstance().shuffle_on != true) {
                                pos += 1;
                                song = SongQueue.getInstance().song_list.get(pos);
                            } else {
                                while (song == SongQueue.getInstance().current_song) {
                                    pos = rand.nextInt(SongQueue.getInstance().song_list.size());
                                    song = SongQueue.getInstance().song_list.get(pos);
                                }
                            }
                        } else {
                            ;
                        }
                    }
                    //Skipping song
                    SongQueue.getInstance().addSong(song);
                    SongQueue.getInstance().setPosition(pos);
                    updateNotification(song);
                    playMusic(song);
                    handleEnd();
                }  else {
                    ;
                }
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
    }
}
