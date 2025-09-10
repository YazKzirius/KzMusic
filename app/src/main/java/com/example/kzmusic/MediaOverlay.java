package com.example.kzmusic;
//Imports
import static androidx.work.LoggerExtKt.logd;

import java.util.Arrays;
import java.util.Random;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.net.Uri;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.ExoPlayer;
import android.media.audiofx.EnvironmentalReverb;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.ImageButton;
import com.bumptech.glide.Glide;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MediaOverlay#newInstance} factory method to
 * create an instance of this fragment.
 */
//This implements the playback overlay when a song is clicked
public class MediaOverlay extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    //Important attributes
    View view;
    MusicFile musicFile;
    int position;
    private boolean openedFromNotification = false;

    private List<MusicFile> musicFiles = new ArrayList<>();
    //UI Attributes
    private TextView overlaySongTitle;
    private ImageButton btnPlayPause;
    private Handler handler = new Handler();
    Boolean is_looping = false;
    Boolean shuffle_on = false;
    private ImageButton btnLoop;
    private ImageButton btnSkip_left;
    private ImageButton btnSkip_right;
    private ImageButton btnShuffle;
    private SeekBar seekBar;
    private SeekBar seekBarReverb;
    private SeekBar seekBarSpeed;
    int session_id;
    private ExoPlayer player;
    private TextView speed_text;
    private TextView reverb_text;
    private TextView textCurrentTime, textTotalDuration;
    private ImageView album_cover;
    private ImageView song_gif;
    float song_speed = (float) 1.0;
    float song_pitch = (float) 1.0;
    private SharedViewModel sharedViewModel;
    private PlayerService playerService;
    boolean isBound = false;
    ServiceConnection serviceConnection;
    SeekBar seekBarPitch;
    TextView pitch_text;
    private Handler uiUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable uiUpdateRunnable;
    private boolean isDurationSet = false; // A flag to track if we've set the duration for the current song.
    private VisualizerView visualizerView;
    private Handler visualizerHandler;
    private Runnable visualizerRunnable;
    private float[] fftData;
    private boolean isVisualizerRunning = false; // A crucial flag to control the loop's state
    private Float reverb;

    public MediaOverlay() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MediaOverlay.
     */
    // TODO: Rename and change types and number of parameters
    public static MediaOverlay newInstance(String param1, String param2) {
        MediaOverlay fragment = new MediaOverlay();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //Pausing spotify player if song is currently playing, to elimnate overlap
        if (OnlinePlayerManager.getInstance().mSpotifyAppRemote != null) {
            OnlinePlayerManager.getInstance().pause_playback();
        }
        //Implementing player functionality
        view = inflater.inflate(R.layout.fragment_media_overlay, container, false);
        overlaySongTitle = view.findViewById(R.id.songTitle);
        Animation scrollAnim = AnimationUtils.loadAnimation(getContext(), R.anim.scroll_horizontal);
        overlaySongTitle.startAnimation(scrollAnim);
        album_cover = view.findViewById(R.id.musicImage);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnLoop = view.findViewById(R.id.btnLoop);
        btnSkip_left = view.findViewById(R.id.btnSkipLeft);
        btnSkip_right = view.findViewById(R.id.btnSkipRight);
        btnShuffle = view.findViewById(R.id.btnShuffle);
        speed_text = view.findViewById(R.id.speed_text);
        pitch_text = view.findViewById(R.id.pitch_text);
        reverb_text = view.findViewById(R.id.reverb_text);
        seekBar = view.findViewById(R.id.seekBar);
        seekBarReverb = view.findViewById(R.id.seekBarReverb);
        seekBarSpeed = view.findViewById(R.id.seekBarSpeed);
        seekBarPitch = view.findViewById(R.id.seekBarPitch);
        textCurrentTime = view.findViewById(R.id.textCurrentTime);
        textTotalDuration = view.findViewById(R.id.textTotalDuration);
        visualizerView = view.findViewById(R.id.visualizerView);
        //Retrieving data from song queue
        if (SongQueue.getInstance().current_song == null) {
            ;
        } else {
            musicFile = SongQueue.getInstance().current_song;
            position = SongQueue.getInstance().current_position;
            is_looping = SongQueue.getInstance().is_looping;
            shuffle_on = SongQueue.getInstance().shuffle_on;
            fftData = new float[512];
            visualizerHandler = new Handler(Looper.getMainLooper());
            //Setting up notification
            startPlayerService();
            musicFiles = SongQueue.getInstance().song_list;
            //Setting up skipping;
            set_up_skipping();
            //Playing music
            set_up_view(musicFile);
            //Setting up media buttons
            set_up_media_buttons();
            set_up_other_buttons();
            //Setting up speed+pitch seekbar functionality
            set_up_speed_and_pitch();
            //Setting up reverberation seekbar functionality
            set_up_reverb();
            //Setting up menu
            set_up_top_menu();
            add_animation();
        }
        return view;
    }
    //This function adds animation to UI
    public void add_animation() {
        LinearLayout header = view.findViewById(R.id.down_btn).getRootView().findViewById(R.id.down_btn).getParent() instanceof LinearLayout
                ? (LinearLayout) view.findViewById(R.id.down_btn).getParent()
                : null;

        if (header != null) {
            header.setTranslationY(-100f);
            header.setAlpha(0f);
            header.animate().translationY(0f).alpha(1f).setDuration(500).start();
        }
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(album_cover, "scaleX", 1f, 1.07f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(album_cover, "scaleY", 1f, 1.07f);
        scaleUpX.setRepeatMode(ValueAnimator.REVERSE);
        scaleUpY.setRepeatMode(ValueAnimator.REVERSE);
        scaleUpX.setRepeatCount(ValueAnimator.INFINITE);
        scaleUpY.setRepeatCount(ValueAnimator.INFINITE);
        scaleUpX.setDuration(1600);
        scaleUpY.setDuration(1600);
        scaleUpX.start();
        scaleUpY.start();
    }

    //This button sets up pop up menu display
    public void set_up_top_menu() {
        ImageButton down = view.findViewById(R.id.down_btn);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = requireActivity().getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                }

            }
        });
        ImageButton menu = view.findViewById(R.id.menu_btn2);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });
    }

    //This function sets up music image view
    public void set_up_circular_view(MusicFile file) {
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(file.getAlbumId()));
        //Loading images into views
        Glide.with(getContext())
                .asBitmap()
                .load(album_uri)
                .circleCrop().error(R.drawable.logo)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Set the loaded image
                        album_cover.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle the case when the image is cleared (e.g., when the view is recycled)
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // Load a backup image if the main image fails to load
                        if (album_cover != null && album_cover.getContext() != null) {
                            Bitmap defaultIcon = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.logo);
                            album_cover.setImageBitmap(getCircularBitmap(defaultIcon));
                        }

                    }
                });
        if (playerService != null){
            if (playerService.isCurrentlyPlaying()) {
                startVisualizer();
            } else {
                stopVisualizer();
            }
        }
    }
    //This function cirle crops a bitmap image for the logo
    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return output;
    }
    private void startVisualizer() {
        // 1. Check the flag. If the loop is already running, do nothing.
        //    This prevents creating multiple, conflicting update loops.
        if (isVisualizerRunning) return;
        isVisualizerRunning = true; // 2. Set the flag to indicate the loop is now active.

        // 3. Define the task to be run repeatedly (if it's the first time).
        if (visualizerRunnable == null) {
            visualizerRunnable = new Runnable() {
                @Override
                public void run() {
                    // The drawing logic only executes if the loop is active and all components are ready.
                    if (isVisualizerRunning && isBound && playerService != null && playerService.isCurrentlyPlaying() && visualizerView != null) {
                        // Get the latest audio frequency data from the C++ engine.
                        playerService.getLatestFftData(fftData);
                        // Pass the data to our custom view to trigger a redraw.
                        visualizerView.updateVisualizer(fftData);
                    }

                    // THE KEY FIX: The re-posting logic is controlled by the flag.
                    // As long as the flag is true, the loop will schedule itself to run again.
                    if (isVisualizerRunning) {
                        visualizerHandler.postDelayed(this, 33); // Schedule next frame (~30 FPS)
                    }
                }
            };
        }
        // 4. Start the loop.
        visualizerHandler.post(visualizerRunnable);
    }
    private void stopVisualizer() {
        // 1. Set the flag to false. This will cause the runnable to stop re-posting itself on its next execution.
        isVisualizerRunning = false;
        // 2. Explicitly remove any pending callbacks from the handler to stop the loop immediately.
        if (visualizerHandler != null) {
            visualizerHandler.removeCallbacksAndMessages(null);
        }
    }
    private void setRandomVisualizerMode() {
        Random visualizerRandomizer = new Random();
        // Safety check to prevent crashes if the view isn't ready
        if (visualizerView == null) {
            return;
        }

        // Get an array of all possible modes from the VisualizerMode enum
        VisualizerView.VisualizerMode[] allModes = VisualizerView.VisualizerMode.values();

        // Pick a random index from the array
        int randomIndex = visualizerRandomizer.nextInt(allModes.length);

        // Get the randomly selected mode
        VisualizerView.VisualizerMode randomMode = allModes[randomIndex];

        // Apply the new mode to the view
        visualizerView.setVisualizerMode(randomMode);
    }

    //This function sets up media notification bar skip events
    public void set_up_skipping() {
        //Connecting to service
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
                playerService = binder.getService();
                if (playerService != null) {
                    //Playing new song
                    playerService.play_advanced_Music(musicFile);
                    //This updates notification ui every new call
                    playerService.updatePlaybackState();
                    playerService.updateNotification(musicFile);
                    playerService.handlePlay();
                    isBound = true;
                    // Pass the ViewModel to the service
                    sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
                    playerService.setViewModel(sharedViewModel);
                    // Now you can call service methods
                    set_up_circular_view(musicFile);
                    //Setting up seekbar
                    set_up_bar();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        Intent intent = new Intent(getActivity(), PlayerService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Observe the skip event
        sharedViewModel.getSkipEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                Boolean shouldSkip = event.getContentIfNotHandled();
                if (shouldSkip != null && shouldSkip) {
                    if (SongQueue.getInstance().current_song == null) {
                        ;
                    } else {
                        // Handle the skip event in the fragment
                        musicFile = SongQueue.getInstance().current_song;
                        //Initializing reverb from Song manager class
                        String display_title = musicFile.getName();
                        String artist = musicFile.getArtist().replaceAll("/", ", ");
                        display_title = display_title.replaceAll("by " + artist, "").replaceAll(
                                "- " + artist, "");
                        if (isOnlyDigits(display_title)) {
                            display_title = display_title + " by " + artist;
                        } else {
                            display_title = format_title(display_title) + " by " + artist;
                        }
                        overlaySongTitle.setText(display_title);
                        if (playerService != null) {
                            playerService.updatePlaybackState();
                            playerService.updateNotification(musicFile);
                            playerService.handlePlay();
                        }
                        //Displaying circular view
                        set_up_circular_view(musicFile);
                        //Setting up seekbar
                        set_up_bar();
                    }
                } else {
                    ;
                }
            }
        });
        //Setting up for Pause events
        sharedViewModel.getPauseEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                Boolean shouldPlayPause = event.getContentIfNotHandled();
                if (shouldPlayPause != null && shouldPlayPause) {
                    // Stop the GIF by clearing the ImageView
                    startVisualizer();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    ;
                }
            }
        });
        //Setting up for Play events
        sharedViewModel.getPlayEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                Boolean shouldPlayPause = event.getContentIfNotHandled();
                if (shouldPlayPause != null && shouldPlayPause) {
                    // Stop the GIF by clearing the ImageView
                    startVisualizer();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    set_up_circular_view(musicFile);
                } else {
                    ;
                }
            }
        });
        //Setting up for End events
        sharedViewModel.getEndEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                Boolean End = event.getContentIfNotHandled();
                if (End != null && End) {
                    if (SongQueue.getInstance().current_song == null) {
                        ;
                    } else {
                        musicFile = SongQueue.getInstance().current_song;
                    }
                }
            }
        });
        //Setting up for seekbar events
        sharedViewModel.getUpdateEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                set_up_bar();
            }
        });
    }
    // This function sets up other media buttons (Rewind/Forward)
    public void set_up_other_buttons() {
        ImageButton rewind = view.findViewById(R.id.btnRewind);
        ImageButton forward = view.findViewById(R.id.btnForward);

        rewind.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            if (playerService != null) {
                long newPosition = playerService.getCurrentPosition() - 5000; // 5 seconds back
                playerService.seekToPosition(Math.max(0, newPosition)); // Don't seek before 0
            }
        });

        forward.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            if (playerService != null) {
                long newPosition = playerService.getCurrentPosition() + 5000; // 5 seconds forward
                long duration = playerService.getTrackDuration();
                // Don't seek past the end of the song
                if (duration > 0) {
                    playerService.seekToPosition(Math.min(newPosition, duration));
                }
            }
        });
    }

    // This function sets up and implements the main media button functionality
    public void set_up_media_buttons() {
        // --- Play/Pause Button ---
        btnPlayPause.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            if (playerService == null) return;

            if (playerService.isCurrentlyPlaying()) {
                playerService.pause(); // Call service method
                playerService.updatePlaybackState();
                btnPlayPause.setImageResource(R.drawable.ic_play);
                // Notification updates should be handled inside the service's pause() method
                stopVisualizer();
            } else {
                playerService.play(); // Call service method
                playerService.updatePlaybackState();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                // Notification updates should be handled inside the service's play() method
                startVisualizer();
            }
        });

        // --- Loop Button ---
        btnLoop.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            is_looping = !is_looping; // Toggle local state
            SongQueue.getInstance().setIs_looping(is_looping);

            if (playerService != null) {
                playerService.enableLooping(is_looping); // Tell the native engine to loop
            }

            // Update UI
            btnLoop.setImageResource(is_looping ? R.drawable.ic_loop_on : R.drawable.ic_loop);
            // Service should handle its own notification updates
        });
        // Set initial state
        btnLoop.setImageResource(is_looping ? R.drawable.ic_loop_on : R.drawable.ic_loop);


        // --- Skip Left/Right and Shuffle Buttons
        // The only change is to call playerService.pause() instead of player.pause()
        Random rand = new Random();
        btnSkip_left.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            if (playerService == null || musicFiles == null || musicFiles.isEmpty()) return;
            playerService.pause(); // Use service to pause
            // track selection logic ..
            if (musicFiles.contains(musicFile)) {
                position = musicFiles.indexOf(musicFile);
            } else {
                position = SongQueue.getInstance().current_position;;
            }
            if (!shuffle_on) {
                position = (position == 0) ? musicFiles.size() - 1 : position - 1;
            } else {
                position = rand.nextInt(musicFiles.size());
            }
            if (position < 0 || position >= musicFiles.size()) {
                ;
            } else {
                musicFile = musicFiles.get(position);
                SongQueue.getInstance().addSong(musicFile);
                SongQueue.getInstance().setPosition(position);
                if (playerService != null && musicFile != null) {
                    playerService.updateNotification(musicFile);
                    playerService.play_advanced_Music(musicFile);
                    set_up_view(musicFile);
                }
            }

        });

        btnSkip_right.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            if (playerService == null || musicFiles == null || musicFiles.isEmpty()) return;
            playerService.pause(); // Use service to pause
            //  track selection logic ...
            if (musicFiles.contains(musicFile)) {
                position = musicFiles.indexOf(musicFile);
            } else {
                position = SongQueue.getInstance().current_position;;
            }
            if (!shuffle_on) {
                position = (position == 0) ? musicFiles.size() - 1 : position + 1;
            } else {
                position = rand.nextInt(musicFiles.size());
            }
            if (position < 0 || position >= musicFiles.size()) {
                ;
            } else {
                musicFile = musicFiles.get(position);
                SongQueue.getInstance().addSong(musicFile);
                SongQueue.getInstance().setPosition(position);
                if (playerService != null && musicFile != null) {
                    playerService.updateNotification(musicFile);
                    playerService.play_advanced_Music(musicFile);
                    set_up_view(musicFile);
                }
            }
        });

        btnShuffle.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            shuffle_on = !shuffle_on;
            SongQueue.getInstance().setShuffle_on(shuffle_on);
            btnShuffle.setImageResource(shuffle_on ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
        });
        // Set initial state
        btnShuffle.setImageResource(shuffle_on ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
    }
    // This function will be called when a NEW song is played to reset the state.
    public void prepareForNewSong() {
        // Reset the duration text and seekbar max.
        textTotalDuration.setText("00:00");
        seekBar.setMax(0);
        seekBar.setProgress(0);
        // Reset our flag so the updater knows it needs to get the duration again.
        isDurationSet = false;
    }

    // This function now only sets up the listener. The duration is set in the updater.
    public void set_up_bar() {
        if (playerService == null) return;
        long duration = playerService.getTrackDuration();
        if (duration > 0) {
            textTotalDuration.setText(formatTime(duration));
            seekBar.setMax((int) duration);
            isDurationSet = true; // Mark that we've set it for this song.
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playerService != null) {
                    playerService.seekToPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekBarUpdate(); // Pause UI updates while user is dragging
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startSeekBarUpdate(); // Resume UI updates when user lets go
            }
        });

        startSeekBarUpdate(); // Start the periodic UI updates
    }

    // This function now handles BOTH duration setup and progress updates.
    private void startSeekBarUpdate() {
        stopSeekBarUpdate(); // Ensure no multiple runnables are going

        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (playerService != null) {
                    // Always update the current position if the player is playing.
                    if (!isDurationSet ) {
                        long duration = playerService.getTrackDuration();
                        if (duration > 0) {
                            textTotalDuration.setText(formatTime(duration));
                            seekBar.setMax((int) duration);
                            isDurationSet = true; // Mark that we've set it for this song.
                        }
                    }
                    // Always update the current position if the player is playing.
                    if (playerService.isCurrentlyPlaying()) {
                        long currentPosition = playerService.getCurrentPosition();
                        seekBar.setProgress((int) currentPosition);
                        textCurrentTime.setText(formatTime(currentPosition));
                        long duration = playerService.getTrackDuration();
                        if (duration > 0) {
                            textTotalDuration.setText(formatTime(duration));
                            seekBar.setMax((int) duration);
                            isDurationSet = true; // Mark that we've set it for this song.
                        }
                    }
                }
                // Post again for the next update.
                uiUpdateHandler.postDelayed(this, 500); // Update every 500ms
            }
        };
        uiUpdateHandler.post(uiUpdateRunnable);
    }

    // This function stops the seek bar updates
    private void stopSeekBarUpdate() {
        if (uiUpdateHandler != null && uiUpdateRunnable != null) {
            uiUpdateHandler.removeCallbacks(uiUpdateRunnable);
        }
    }

    // Helper function for formatting time (your existing code is fine)
    private String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    //This function plays the specified music file
    private void set_up_view(MusicFile musicFile) {
        //Initializing song properties
        String display_title = musicFile.getName();
        String artist = musicFile.getArtist().replaceAll("/", ", ");
        display_title = display_title.replaceAll("by " + artist, "").replaceAll(
                "- " + artist, "");
        if (isOnlyDigits(display_title)) {
            display_title = display_title + " by " + artist;
        } else {
            display_title = format_title(display_title) + " by " + artist;
        }
        //Applying audio effects
        apply_audio_effect();
        overlaySongTitle.setText(display_title);
        //Displaying circular view
        set_up_circular_view(musicFile);
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
            if (tempCleanedOriginal.isEmpty() || tempCleanedOriginal.matches("^[\\s.-]*$") || isOnlyDigits(tempCleanedOriginal)) {
                // If the original itself was essentially just a watermark/number/extension, returning an empty string might be fine
                // or return the numeric part if that's all that's left of the original.
                if (isOnlyDigits(tempCleanedOriginal) && !tempCleanedOriginal.isEmpty())
                    return tempCleanedOriginal;
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

    //This function assigns audio effects to the exoplayer like speed/reverb
    public void apply_audio_effect() {
        //Initialising audio settings
        song_speed = SongQueue.getInstance().speed;
        song_pitch = SongQueue.getInstance().pitch;
        reverb = SongQueue.getInstance().reverb;
        //Setting playback speed properties
        //Setting up speed+pitch seekbar
        seekBarSpeed.setMax(200);
        seekBarSpeed.setMin(50);
        seekBarSpeed.setProgress((int) (song_speed * 100));
        speed_text.setText("Speed: " + String.format("%.1fx", song_speed));
        seekBarPitch.setMax(200);
        seekBarPitch.setMin(50);
        seekBarPitch.setProgress((int) (song_pitch * 100));
        pitch_text.setText("Pitch: " + String.format("%.1fx", song_pitch));
        //Setting up reverb seekbar
        seekBarReverb.setMax(100);
        seekBarReverb.setMin(0);
        seekBarReverb.setProgress(reverb.intValue());
        reverb_text.setText("Reverberation: "+reverb.intValue()+"%");
    }
    //This function sets up speed manager seek bar
    public void set_up_speed_and_pitch() {
        // SeekBar for Speed
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Setting speed between 0.5x and 2.0x
                float speed = Math.max(0.5f, Math.min(progress / 100f, 2.0f));
                song_speed = speed;
                speed_text.setText("Speed: " + String.format("%.1fx", speed)); // Update the speed text
                if (player != null) {
                    player.setPlaybackParameters(new PlaybackParameters(song_speed, song_pitch));
                }
                seekBarSpeed.setProgress(progress);
                SongQueue.getInstance().setSpeed(song_speed);
                SongQueue.getInstance().setPitch(song_pitch);
                if (playerService != null) {
                    playerService.apply_advanced_audio_effects();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBarPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Setting speed between 0.5x and 2.0x
                float pitch = Math.max(0.5f, Math.min(progress / 100f, 2.0f));
                song_pitch = pitch;
                pitch_text.setText("Pitch: " + String.format("%.1fx", pitch)); // Update the speed text
                if (player != null) {
                    player.setPlaybackParameters(new PlaybackParameters(song_speed, song_pitch));
                }
                seekBarPitch.setProgress(progress);
                SongQueue.getInstance().setSpeed(song_speed);
                SongQueue.getInstance().setPitch(song_pitch);
                if (playerService != null) {
                    playerService.apply_advanced_audio_effects();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    //This function sets up pitch manager seek bar
    public void set_up_reverb() {
        seekBarReverb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Set reverb parameters
                setReverbPreset(progress);
                SongQueue.getInstance().setReverb_level((float) progress);
                reverb_text.setText("Reverberation: "+progress+"%");
                if (playerService != null) {
                    if (progress == 0) {
                        playerService.setEnabled(false);
                    } else {
                        playerService.initialiseReverb((float) progress);
                        playerService.setEnabled(true);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    //This function sets reverb level based on seekbar progress level
    private void setReverbPreset(int progress) {
        //Computing reverberation parameters based of reverb level data proportionality
        try {
            //Estimating percentage of seekbar complete
            ;
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (uiUpdateHandler != null && uiUpdateRunnable != null) {
            handler.removeCallbacks(uiUpdateRunnable);
        }

    }

    //This function shows pop up menu when button is clicked
    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.song_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_go_to_artist:
                        Toast.makeText(getContext(), "Go to artist selected", Toast.LENGTH_SHORT).show();
                        // Add your logic here
                        return true;
                    case R.id.menu_go_to_album:
                        Toast.makeText(getContext(), "Go to album selected", Toast.LENGTH_SHORT).show();
                        // Add your logic here
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }

    private void startPlayerService() {
        Intent intent = new Intent(requireContext(), PlayerService.class);
        requireContext().startService(intent);
    }

    private void stopPlayerService() {
        Intent intent = new Intent(requireContext(), PlayerService.class);
        requireContext().stopService(intent);
    }
}