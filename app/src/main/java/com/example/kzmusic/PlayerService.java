package com.example.kzmusic;

//Imports
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
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

import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.C;
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
    private final IBinder binder = new LocalBinder();
    // Handler for detecting end-of-song and other periodic updates
    private Handler playbackStateHandler = new Handler(Looper.getMainLooper());
    private Runnable playbackStateRunnable;

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }
    static {
        System.loadLibrary("native-lib");
    }
    private native void initSuperpowered(int samplerate, int buffersize);
    private native void openFile(String path);
    // Your native method declarations
    native void play(); // We will add this for clarity
    native void setPitchShift(int cents);
    native void setTempo(double rate);
    native void pause();
    // Add the corresponding native declarations at the top of the class
    native void seekTo(double positionMs);
    native boolean isPlaying();
    native double getPositionMs();
    native double getDurationMs();
    native void setLooping(boolean isLooping);
    native int getPlayerEvent();

    // Now, create public wrapper methods that your UI can call
    public void seekToPosition(long positionMs) {
        seekTo((double)positionMs);
    }

    public boolean isCurrentlyPlaying() {
        return isPlaying();
    }

    public long getCurrentPosition() {
        return (long) getPositionMs();
    }

    public long getTrackDuration() {
        return (long) getDurationMs();
    }

    public void enableLooping(boolean enable) {
        setLooping(enable);
    }
    // Define the event constant. The value '10' comes from the SuperpoweredAdvancedAudioPlayer.h header file.
    public static final int PLAYER_EVENT_OPENED = 10;
    // Create the public wrapper method
    public int getLatestPlayerEvent() {
        return getPlayerEvent();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        //Stopping all notification sessions for single session management
        if (OfflinePlayerManager.getInstance().get_size() > 0) {
            OfflinePlayerManager.getInstance().StopAllSessions();
        }
        Log.d("Player Service", "Service created");
        //Playing the song
        NOTIFICATION_ID = SongQueue.getInstance().NOTIFICATION_ID;
        CHANNEL_ID += NOTIFICATION_ID;
        createNotificationChannel();
        initializeMediaSession();
        sharedViewModel = SharedViewModelProvider.getViewModel(this);
        // Get the native sample rate and buffer size of the device.
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        String framesPerBufferStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int samplerate = Integer.parseInt(sampleRateStr);
        int buffersize = Integer.parseInt(framesPerBufferStr);

        // Initialize the Superpowered C++ engine.
        initSuperpowered(samplerate, buffersize);
    }

// ... and other JNI methods for effects and control.

    /**
     * Plays the specified music file using the Superpowered native audio engine.
     *
     * @param musicFile The MusicFile object containing the path to the song.
     */
    public void play_advanced_Music(MusicFile musicFile) {
        if (musicFile == null || musicFile.getPath() == null) {
            return; // Do nothing if the file is invalid.
        }

        // 1. Open the audio file in the native player.
        // This will stop any currently playing track.
        openFile(musicFile.getPath());

        // 2. Update your application's internal state (queue, history, etc.).
        add_song(musicFile);
        update_song_history(musicFile);

        // --- (3) APPLY AUDIO EFFECTS ---
        // Apply the desired tempo and pitch shift *before* starting playback.
        // These settings will persist until they are changed again.

        // 4. Start playback with the new effects applied.
        play();
        apply_advanced_audio_effects();
        enable_endless_stream();
    }
    //This function applies advanced audio effects to the music file
    public void apply_advanced_audio_effects() {
        double pitch_cents = (SongQueue.getInstance().pitch - 1.0) * 1000;
        setPitchShift((int) pitch_cents);
        setTempo(SongQueue.getInstance().speed);
    }
    //This function adds new song to firestore collection
    public void add_song(MusicFile musicFile) {
        if (musicFile == null || musicFile.getName() == null || musicFile.getArtist() == null) {
            Log.e("MediaInfo", "musicFile, name, or artist is null");
            return;
        }

        String displayTitle = musicFile.getName();
        String artist = musicFile.getArtist().replaceAll("/", ", ");

        // Remove redundant artist mentions from title
        displayTitle = displayTitle
                .replaceAll("by " + Pattern.quote(artist), "")
                .replaceAll("- " + Pattern.quote(artist), "")
                .trim();

        // Format title
        if (isOnlyDigits(displayTitle)) {
            displayTitle = displayTitle + " by " + artist;
        } else {
            displayTitle = format_title(displayTitle) + " by " + artist;
        }

        // Get user email from session
        SessionManager sessionManager = new SessionManager(getApplicationContext());
        String email = sessionManager.getEmail();

        if (email == null || email.isEmpty()) {
            Log.e("Session", "Email not found in session manager");
            return;
        }

        SongDao songDao = AppDatabase.getDatabase(getApplicationContext()).songDao();
        String finalTitle = displayTitle;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Song existing = songDao.getSongByEmailAndTitle(email, finalTitle);
            if (existing == null) {
                Song song = new Song();
                song.email = email;
                song.title = finalTitle;
                song.artist = artist;
                songDao.insert(song);
                Log.d("RoomDB", "✅ New song inserted");
            } else {
                songDao.incrementTimesPlayed(email, finalTitle);
                Log.d("RoomDB", "🔁 Song already exists — times played incremented");
            }
        });
    }
    public void update_song_history(MusicFile musicFile) {
        if (musicFile == null) {
            ;
        } else {
            SongQueue.getInstance().update_history(musicFile);
        }
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
    // --- ViewModel and Event Handlers (Unchanged) ---
    public void handleSkip() { if (sharedViewModel != null) sharedViewModel.triggerSkipEvent(); }
    public void handleEnd() { if (sharedViewModel != null) sharedViewModel.triggerEndEvent(); }
    public void handlePause() { if (sharedViewModel != null) sharedViewModel.triggerPauseEvent(); }
    public void handlePlay() { if (sharedViewModel != null) sharedViewModel.triggerPlayEvent(); }
    public void handleUpdate() { if (sharedViewModel != null) sharedViewModel.triggerUpdateEvent(); }
    public void handleStop() { if (sharedViewModel != null) sharedViewModel.triggerStopEvent(); }
    public void setViewModel(SharedViewModel viewModel) { this.sharedViewModel = viewModel; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This is where you would handle intents if you started the service directly.
        // For MediaSession, the callback is the primary entry point.
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_NOT_STICKY;
    }

    // --- MediaSession and Notification Initialization (Refactored for Superpowered) ---
    public void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(this, "SuperpoweredMediaSession");
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO
                );
        mediaSession.setPlaybackState(stateBuilder.build());

        // This callback is the new "brain". It receives commands and calls our native methods.
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                handlePlay();
                if (isCurrentlyPlaying()) {
                    ;
                } else {
                    play(); // Call service method
                }
                updatePlaybackState();
                handlePlay();
            }

            @Override
            public void onPause() {
                if (isCurrentlyPlaying()) {
                    pause();
                } else {
                    ;
                }
                updatePlaybackState();
                handlePause();
            }

            @Override
            public void onSkipToNext() {
                // Your excellent track selection logic, now decoupled from ExoPlayer.
                if (SongQueue.getInstance().song_list == null || SongQueue.getInstance().song_list.isEmpty()) return;
                Random rand = new Random();
                int pos;
                if (SongQueue.getInstance().song_list.contains(SongQueue.getInstance().current_song)) {
                    pos = SongQueue.getInstance().song_list.indexOf(SongQueue.getInstance().current_song);
                } else {
                    pos = SongQueue.getInstance().current_position;
                }
                if (!SongQueue.getInstance().shuffle_on) {
                    pos += 1;
                    if (SongQueue.getInstance().current_position == 0) {
                        pos  = SongQueue.getInstance().song_list.size() - 1;
                    }
                } else {
                    pos = rand.nextInt(SongQueue.getInstance().song_list.size());
                }
                if (pos < 0 || pos >= SongQueue.getInstance().song_list.size()) {
                    pos  = 0;
                } else {
                    MusicFile nextSong = SongQueue.getInstance().song_list.get(pos);
                    SongQueue.getInstance().addSong(nextSong);
                    SongQueue.getInstance().setPosition(pos);
                    play_advanced_Music(nextSong); // This is your method that starts a new track
                    handleSkip();
                }

            }

            @Override
            public void onSkipToPrevious() {
                if (SongQueue.getInstance().song_list == null || SongQueue.getInstance().song_list.isEmpty()) return;
                Random rand = new Random();
                int pos;
                if (SongQueue.getInstance().song_list.contains(SongQueue.getInstance().current_song)) {
                    pos = SongQueue.getInstance().song_list.indexOf(SongQueue.getInstance().current_song);
                } else {
                    pos = SongQueue.getInstance().current_position;
                }
                if (!SongQueue.getInstance().shuffle_on) {
                    pos -= 1;
                    if (SongQueue.getInstance().current_position == 0) {
                        pos  = SongQueue.getInstance().song_list.size() - 1;
                    }
                } else {
                    pos = rand.nextInt(SongQueue.getInstance().song_list.size());
                }
                if (pos < 0 || pos >= SongQueue.getInstance().song_list.size()) {
                    pos  = 0;
                } else {
                    MusicFile prevSong = SongQueue.getInstance().song_list.get(pos);
                    SongQueue.getInstance().addSong(prevSong);
                    SongQueue.getInstance().setPosition(pos);
                    play_advanced_Music(prevSong);
                    handleSkip();
                }
            }
        });

        mediaSession.setActive(true);
    }

    // This single method now replaces the old `updatePlaybackState(int state)`
    public void updatePlaybackState() {
        if (mediaSession == null) return;
        int state = isCurrentlyPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long position = getCurrentPosition(); // Get position from native engine

        stateBuilder.setState(state, position, 1.0f); // Speed is 1.0 for the session state
        mediaSession.setPlaybackState(stateBuilder.build());

        // Update the notification every time the state changes.
        showNotification(stateBuilder.build());
    }


    // --- Notification Management (Refactored) ---

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Media playback", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    // This replaces your old `showNotification` and is the single source for building it.
    public void showNotification(PlaybackStateCompat state) {
        MusicFile currentSong = SongQueue.getInstance().current_song;
        if (currentSong == null) {
            stopForeground(true); // No song, no notification
            return;
        }

        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseTitle = isPlaying ? "Pause" : "Play";

        // Your metadata formatting logic is good.
        String display_title = format_title(currentSong.getName());
        String artist = currentSong.getArtist();

        Intent intent = new Intent(getApplicationContext(), MainPage.class);
        intent.putExtra("openMediaOverlay", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(display_title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.library)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_skipleft, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(playPauseIcon, playPauseTitle, MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_skipright, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        // Asynchronously load album art and then display the notification.
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(currentSong.getAlbumId()));

        Glide.with(this).asBitmap().load(album_uri).error(R.drawable.logo)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        builder.setLargeIcon(resource);
                        startForeground(NOTIFICATION_ID, builder.build());
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    // This function can be kept for simplicity if other parts of your app call it.
    // It now just calls the main notification method.
    public void updateNotification(MusicFile musicFile) {
        // The main `updatePlaybackState` handles refreshing the notification.
        // We can force an update if needed.
        if (mediaSession != null) {
            showNotification(mediaSession.getController().getPlaybackState());
        }
    }


    // --- End-of-Song Handling (Replaces ExoPlayer Listener) ---

    // This function replaces `enable_endless_stream` and `simulateEndOfSong`
    public void enable_endless_stream() {
        stopPlaybackStateUpdater(); // Ensure only one is running
        playbackStateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCurrentlyPlaying()) {
                    long currentPosition = getCurrentPosition();
                    long duration = getTrackDuration();

                    // Check if the song has finished. We add a small buffer (500ms) to prevent premature skipping.
                    if (duration > 0 && currentPosition >= (duration - 500)) {
                        handleEnd();
                        // Use the MediaSession callback to handle the skip logic
                        mediaSession.getController().getTransportControls().skipToNext();
                    } else {
                        // Song is still playing, schedule the next check
                        playbackStateHandler.postDelayed(this, 1000); // Check every second
                    }
                }
            }
        };
        playbackStateHandler.post(playbackStateRunnable);
    }

    // Helper to stop the updater
    public void stopPlaybackStateUpdater() {
        if (playbackStateHandler != null && playbackStateRunnable != null) {
            playbackStateHandler.removeCallbacks(playbackStateRunnable);
        }
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
