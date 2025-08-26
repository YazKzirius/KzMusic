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
    private static final String ACTION_SKIP_NEXT = "com.example.ACTION_SKIP_NEXT";
    private static final String ACTION_SKIP_PREVIOUS = "com.example.ACTION_SKIP_PREVIOUS";
    private final IBinder binder = new LocalBinder();
    private long last_position;

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
    private native void play(); // We will add this for clarity
    private native void setPitchShift(int cents);
    private native void setTempo(double rate);
    private native void pause();
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
    }
    //This function applies advanced audio effects to the music file
    public void apply_advanced_audio_effects() {
        setPitchShift(300);
        setTempo(1.2);
    }


    //This function plays the specified music file
    public void playMusic(MusicFile musicFile) {
        if (musicFile == null) {
            ;
        } else {
            //Playing resuming song at previous duration if the same song as last
            if (SongQueue.getInstance().get_size() >= 1) {
                int index = SongQueue.getInstance().pointer - 1;
                if (index < 0 || index >= SongQueue.getInstance().get_size()) {
                    ;
                } else {
                    if (SongQueue.getInstance().get_specified(index) == null || SongQueue.getInstance().get_specified(index - 1) == null) {
                        OfflinePlayerManager.getInstance().stopAllPlayers();
                        player = new ExoPlayer.Builder(getApplicationContext()).build();
                        Uri uri = Uri.fromFile(new File(musicFile.getPath()));
                        MediaItem mediaItem = MediaItem.fromUri(uri);
                        player.setMediaItem(mediaItem);
                        add_song(musicFile);
                        update_song_history(musicFile);
                    } else {
                        MusicFile song1;
                        MusicFile song2;
                        song1 = SongQueue.getInstance().get_specified(index);
                        song2 = SongQueue.getInstance().get_specified(index - 1);
                        String s1 = song1.getName();
                        String s2 = song2.getName();
                        if (s1 != null && s1.equals(s2)) {
                            // Resume with previous player
                            player = OfflinePlayerManager.getInstance().current_player;
                        } else {
                            OfflinePlayerManager.getInstance().stopAllPlayers();
                            player = new ExoPlayer.Builder(getApplicationContext()).build();
                            Uri uri = Uri.fromFile(new File(musicFile.getPath()));
                            MediaItem mediaItem = MediaItem.fromUri(uri);
                            player.setMediaItem(mediaItem);
                            add_song(musicFile);
                            update_song_history(musicFile);
                        }
                    }
                }
            } else {
                player = new ExoPlayer.Builder(getApplicationContext()).build();
                Uri uri = Uri.fromFile(new File(musicFile.getPath()));
                MediaItem mediaItem = MediaItem.fromUri(uri);
                player.setMediaItem(mediaItem);
                add_song(musicFile);
                update_song_history(musicFile);
            }
            if (player == null) {
                ;
            } else {
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
        }
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
    //This function assigns audio effects to the exoplayer like speed/reverb
    public void apply_audio_effect() {
        //Initialising reverb settings
        SongQueue.getInstance().initialize_reverb(session_id);
        reverb = SongQueue.getInstance().reverb;
        //Setting playback speed properties
        if (player == null) {
            ;
        } else {
            player.setPlaybackParameters(new PlaybackParameters(SongQueue.getInstance().speed, SongQueue.getInstance().pitch));
        }
        // Apply reverb properties safely
        if (reverb != null) {
            setReverbPreset(SongQueue.getInstance().reverb_level);
        } else {
            Toast.makeText(getApplicationContext(), "Reverb is not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void setReverbPreset(int progress) {
        try {
            // Clamp values safely within supported EnvironmentalReverb range
            if (SongQueue.getInstance().reverb == null) {
                ;
            } else {
                int clampedProgress = Math.max(-9000, Math.min(progress, 2000));
                int roomLevel = Math.max(-9000, Math.min(-2000 + (clampedProgress + 1000), 0));  // roomLevel: -9000 to 0
                int decayTime = 1000 + (clampedProgress + 1000); // e.g. ~2000–3000ms range depending on level

                reverb.setRoomLevel((short) roomLevel);
                reverb.setReverbLevel((short) clampedProgress);
                reverb.setDecayTime(decayTime);
                reverb.setDiffusion((short) 1000);
                reverb.setDensity((short) 1000);
                reverb.setEnabled(true);
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Something went wrong with audio effects", Toast.LENGTH_SHORT).show();
            Log.e("AudioEffect", "Error applying reverb: ", e);
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
        if (viewModel != null) {
            this.sharedViewModel = viewModel;
        }
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
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f);

        mediaSession.setPlaybackState(stateBuilder.build());
        OfflinePlayerManager.getInstance().addSession(mediaSession);
        showNotification(stateBuilder.build());

        Random rand = new Random();

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                ExoPlayer player = OfflinePlayerManager.getInstance().current_player;
                if (player != null) {
                    player.play();
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    showNotification(stateBuilder.build());
                    handlePlay();
                }
                play();
            }

            @Override
            public void onPause() {
                super.onPause();
                ExoPlayer player = OfflinePlayerManager.getInstance().current_player;
                if (player != null) {
                    player.pause();
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    showNotification(stateBuilder.build());
                    handlePause();
                }
                pause();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                int pos;
                if (OfflinePlayerManager.getInstance().current_player == null  ||
                        SongQueue.getInstance().song_list == null || SongQueue.getInstance().song_list.isEmpty()) return;
                if (SongQueue.getInstance().song_list.contains(SongQueue.getInstance().current_song)) {
                    pos = SongQueue.getInstance().song_list.indexOf(SongQueue.getInstance().current_song);
                } else {
                    pos = SongQueue.getInstance().current_position;
                }
                if (!SongQueue.getInstance().shuffle_on) {
                    pos += 1;
                    if (SongQueue.getInstance().current_position == SongQueue.getInstance().song_list.size() - 1) {
                        pos  = 0;
                    }
                } else {
                    pos = rand.nextInt(SongQueue.getInstance().song_list.size());
                }
                if (pos < 0 || pos >= SongQueue.getInstance().song_list.size()) {
                    pos  = 0;
                } else {
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
                if (OfflinePlayerManager.getInstance().current_player == null  ||
                        SongQueue.getInstance().song_list == null || SongQueue.getInstance().song_list.isEmpty()) return;
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
                    MusicFile song = SongQueue.getInstance().song_list.get(pos);
                    SongQueue.getInstance().addSong(song);
                    SongQueue.getInstance().setPosition(pos);
                    updateNotification(song);
                    playMusic(song);
                    handleSkip();
                }
            }
        });

        mediaSession.setActive(true);
    }

    public void updatePlaybackState(int state) {
        //Checking if state is on repeat and implementing functionality
        if (OfflinePlayerManager.getInstance().current_player == null) {
            ;
        } else {
            if (state == PlaybackStateCompat.REPEAT_MODE_ONE) {
                OfflinePlayerManager.getInstance().current_player.setRepeatMode(Player.REPEAT_MODE_ONE);
                stateBuilder.setState(stateBuilder.build().getState(), 0, SongQueue.getInstance().speed);
            } else {
                stateBuilder.setState(state, 0, SongQueue.getInstance().speed);
            }
            mediaSession.setPlaybackState(stateBuilder.build());
        }
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
        if (SongQueue.getInstance().current_song == null) {
            ;
        } else {
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
            // Intent to open MediaOverlay when tapped
            Intent intent = new Intent(getApplicationContext(), MainPage.class);
            intent.putExtra("openMediaOverlay", true); // 👈 Signal to open the fragment
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.library)
                    .setContentTitle(display_title)
                    .setContentText(SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", "))
                    .setContentIntent(pendingIntent)
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
    }
    //This function updates the current notification view holder when a song is skipped
    public void updateNotification(MusicFile musicFile) {
        if (musicFile == null) {
            ;
        } else {
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
            //Intent to open MediaOverlay when tapped
            Intent intent = new Intent(getApplicationContext(), MainPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("openMediaOverlay", true); // 👈 Signal to open the fragment
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE);
            //Updating current notification with new details and meta data
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.library)
                    .setContentTitle(display_title)
                    .setContentText(musicFile.getArtist().replaceAll("/", ", "))
                    .setContentIntent(pendingIntent)
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

    }
    public void simulateEndOfSong() {
        ExoPlayer player = OfflinePlayerManager.getInstance().current_player;
        if (player == null) return;

        long duration = player.getDuration();

        // ExoPlayer may return TIME_UNSET (-9223372036854775807L) or 0 if duration is not ready
        if (duration > 0 && duration != C.TIME_UNSET) {
            player.seekTo(duration); // Nudge playback to the end
        }

        player.stop(); // Stop gracefully (can trigger END event)

        // Be extra safe and release resources to avoid leaks
        player.release();
        OfflinePlayerManager.getInstance().setCurrent_player(null);
    }
    public void enable_endless_stream() {
        Player player = OfflinePlayerManager.getInstance().current_player;
        if (player == null) return;

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) handleUpdate();
                else handleStop();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState != Player.STATE_ENDED) return;

                simulateEndOfSong();

                SongQueue queue = SongQueue.getInstance();
                List<MusicFile> songs = queue.song_list;
                int currentPos = queue.current_position;
                boolean shuffle = queue.shuffle_on;

                if (songs == null || songs.isEmpty()) return;

                int nextPos = currentPos;

                if (!shuffle) {
                    nextPos = currentPos + 1;
                    if (nextPos >= songs.size()) return; // End of list
                } else {
                    Random rand = new Random();
                    MusicFile nextSong;
                    int attempts = 0;
                    do {
                        nextPos = rand.nextInt(songs.size());
                        nextSong = songs.get(nextPos);
                        attempts++;
                    } while ((nextSong == null || nextSong.equals(queue.current_song)) && attempts < 10);
                }

                if (nextPos >= songs.size() || nextPos < 0 || songs.get(nextPos) == null) return;

                MusicFile next = songs.get(nextPos);
                queue.addSong(next);
                queue.setPosition(nextPos);

                playMusic(next);
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                updateNotification(next);
                handleEnd();
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
