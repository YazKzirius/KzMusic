package com.example.kzmusic;
//Imports
import java.util.Random;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
    private EnvironmentalReverb reverb;
    private TextView textCurrentTime, textTotalDuration;
    private ImageView album_cover;
    private ImageView song_gif;
    private Runnable runnable;
    float song_speed = (float) 1.0;
    float song_pitch = (float) 1.0;
    int reverb_level = -1000;
    private SharedViewModel sharedViewModel;
    private PlayerService playerService;
    private boolean isBound = false;
    ServiceConnection serviceConnection;
    private static final String API_KEY = "2ae10e3784a6e96d12c87d11692e8089";
    SessionManager sessionManager;
    long last_position = 0;

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
        song_gif = view.findViewById(R.id.song_playing_gif);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnLoop = view.findViewById(R.id.btnLoop);
        btnSkip_left = view.findViewById(R.id.btnSkipLeft);
        btnSkip_right = view.findViewById(R.id.btnSkipRight);
        btnShuffle = view.findViewById(R.id.btnShuffle);
        speed_text = view.findViewById(R.id.speed_text);
        reverb_text = view.findViewById(R.id.reverb_text);
        seekBar = view.findViewById(R.id.seekBar);
        seekBarReverb = view.findViewById(R.id.seekBarReverb);
        seekBarSpeed = view.findViewById(R.id.seekBarSpeed);
        textCurrentTime = view.findViewById(R.id.textCurrentTime);
        textTotalDuration = view.findViewById(R.id.textTotalDuration);
        //Retrieving data from song queue
        if (SongQueue.getInstance().current_song == null) {
            ;
        } else {
            musicFile = SongQueue.getInstance().current_song;
            position = SongQueue.getInstance().current_position;
            is_looping = SongQueue.getInstance().is_looping;
            shuffle_on = SongQueue.getInstance().shuffle_on;
            //Setting up notification
            startPlayerService();
            //Setting up skipping;
            set_up_skipping();
            //Playing music
            set_up_view(musicFile);
            //Loading previous music files
            loadMusicFiles();
            //Setting up media buttons
            set_up_media_buttons();
            //Setting up speed+pitch seekbar functionality
            set_up_speed_and_pitch();
            //Setting up reverberation seekbar functionality
            set_up_reverb();
            //Setting up menu
            set_up_pop_menu();
            add_animation();
            add_background_animation();
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

    //This function adds background animation
    public void add_background_animation() {

        // Reference your root layout
        FrameLayout rootLayout = view.findViewById(R.id.media_overlay); // If inside Fragment

// Define a large base radius for a spacious glow
        float baseRadius = 1400f;

// Create the radial gradient drawable
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.parseColor("#3A0CA3"),  // vibrant purple edge
                        Color.parseColor("#1A0D2E"),  // deep indigo mid
                        Color.parseColor("#090909")   // dominant black core
                }
        );
        gradient.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gradient.setGradientRadius(baseRadius);
        gradient.setGradientCenter(0.5f, 0.4f); // slightly above center
        gradient.setCornerRadius(0f);

// Apply background to layout
        rootLayout.setBackground(gradient);

// Create pulse animation
        ValueAnimator animator = ValueAnimator.ofFloat(0.8f, 1.3f);
        animator.setDuration(5000); // slow, breathable pulse
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);

// Update radius during animation
        animator.addUpdateListener(animation -> {
            float factor = (float) animation.getAnimatedValue();
            gradient.setGradientRadius(baseRadius * factor);
            rootLayout.invalidate(); // force redraw
        });

// Start the animation
        animator.start();
    }

    //This button sets up pop up menu display
    public void set_up_pop_menu() {
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
                .circleCrop()
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
                            Glide.with(album_cover.getContext())
                                    .asBitmap()
                                    .load(R.drawable.logo)
                                    .circleCrop()
                                    .into(album_cover);
                        }

                    }
                });
        Glide.with(getContext()).asGif().load(R.drawable.media_playing).circleCrop().into(song_gif);
    }

    //This function sets up media notification bar skip events
    public void set_up_skipping() {
        //Connecting to service
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
                playerService = binder.getService();
                //Playing new song
                playerService.playMusic(musicFile);
                //This updates notifcation ui every new call
                playerService.updateNotification(musicFile);
                isBound = true;
                // Pass the ViewModel to the service
                sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
                playerService.setViewModel(sharedViewModel);
                // Now you can call service methods
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
                        player = OfflinePlayerManager.getInstance().current_player;
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
                    if (OfflinePlayerManager.getInstance().current_player == null) {
                        ;
                    } else {
                        player = OfflinePlayerManager.getInstance().current_player;
                        // Stop the GIF by clearing the ImageView
                        Glide.with(getContext()).clear(song_gif);
                        song_gif.setImageDrawable(null);
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                    }
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
                    if (OfflinePlayerManager.getInstance().current_player == null) {
                        ;
                    } else {
                        player = OfflinePlayerManager.getInstance().current_player;
                        // Stop the GIF by clearing the ImageView
                        Glide.with(getContext()).clear(song_gif);
                        song_gif.setImageDrawable(null);
                        btnPlayPause.setImageResource(R.drawable.ic_pause);
                    }
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

    //This function sets up and implements button functionality
    public void set_up_media_buttons() {
        Random rand = new Random();

        btnPlayPause.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            if (player == null) return;

            if (player.isPlaying()) {
                player.pause();
                Glide.with(requireContext()).clear(song_gif);
                song_gif.setImageDrawable(null);
                btnPlayPause.setImageResource(R.drawable.ic_play);
                if (playerService != null) {
                    playerService.updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    playerService.showNotification(playerService.stateBuilder.build());
                }
            } else {
                player.play();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                set_up_circular_view(musicFile);
                if (playerService != null) {
                    playerService.updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    playerService.showNotification(playerService.stateBuilder.build());
                }
            }
        });

        // Loop setup
        //If loop was on previously, keep loop on otherwise, continue
        if (playerService != null) {
            if (is_looping) {
                SongQueue.getInstance().setIs_looping(true);
                playerService.updatePlaybackState(PlaybackStateCompat.REPEAT_MODE_ONE);
                playerService.updateNotification(musicFile);
                btnLoop.setImageResource(R.drawable.ic_loop_on);
            } else {
                SongQueue.getInstance().setIs_looping(false);
                playerService.updatePlaybackState(PlaybackStateCompat.REPEAT_MODE_NONE);
                playerService.updateNotification(musicFile);
                btnLoop.setImageResource(R.drawable.ic_loop);
            }
        } else {
            // Still update local UI and state safely
            SongQueue.getInstance().setIs_looping(is_looping);
            btnLoop.setImageResource(is_looping ? R.drawable.ic_loop_on : R.drawable.ic_loop);
        }
        btnLoop.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();
            // toggle playback...
            //Loop functionality
            is_looping = !is_looping;
            //If loop was on previously, keep loop on otherwise, continue
            if (playerService != null) {
                if (is_looping) {
                    SongQueue.getInstance().setIs_looping(true);
                    playerService.updatePlaybackState(PlaybackStateCompat.REPEAT_MODE_ONE);
                    playerService.updateNotification(musicFile);
                    btnLoop.setImageResource(R.drawable.ic_loop_on);
                } else {
                    SongQueue.getInstance().setIs_looping(false);
                    playerService.updatePlaybackState(PlaybackStateCompat.REPEAT_MODE_NONE);
                    playerService.updateNotification(musicFile);
                    btnLoop.setImageResource(R.drawable.ic_loop);
                }
            } else {
                // Still update local UI and state safely
                SongQueue.getInstance().setIs_looping(is_looping);
                btnLoop.setImageResource(is_looping ? R.drawable.ic_loop_on : R.drawable.ic_loop);
            }

        });
        btnSkip_left.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            if (player == null || musicFiles == null || musicFiles.isEmpty()) return;

            player.pause();

            if (!shuffle_on) {
                position = (position == 0) ? musicFiles.size() - 1 : position - 1;
            } else {
                position = rand.nextInt(musicFiles.size());
            }
            musicFile = musicFiles.get(position);
            SongQueue.getInstance().addSong(musicFile);
            SongQueue.getInstance().setPosition(position);
            if (playerService != null && musicFile != null) {
                playerService.updateNotification(musicFile);
                playerService.playMusic(musicFile);
                set_up_view(musicFile);
            }
        });

        btnSkip_right.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            if (player == null || musicFiles == null || musicFiles.isEmpty()) return;

            player.pause();

            if (!shuffle_on) {
                position = (position == musicFiles.size() - 1) ? 0 : position + 1;
            } else {
                position = rand.nextInt(musicFiles.size());
            }

            musicFile = musicFiles.get(position);
            SongQueue.getInstance().addSong(musicFile);
            SongQueue.getInstance().setPosition(position);
            if (playerService != null && musicFile != null) {
                playerService.updateNotification(musicFile);
                playerService.playMusic(musicFile);
                set_up_view(musicFile);
            }
        });
        //Implementing shuffle button functionality
        //If loop was on previously, keep loop on otherwise, continue
        if (shuffle_on == true) {
            //Setting repeat mode on and replacing icon
            SongQueue.getInstance().setShuffle_on(true);
            btnShuffle.setImageResource(R.drawable.ic_shuffle_on);
        } else {
            //Setting repeat mode off and replacing icon
            SongQueue.getInstance().setShuffle_on(false);
            btnShuffle.setImageResource(R.drawable.ic_shuffle);
        }
        btnShuffle.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();
            // toggle playback...
            shuffle_on = !shuffle_on;
            if (shuffle_on == true) {
                //Setting repeat mode on and replacing icon
                SongQueue.getInstance().setShuffle_on(true);
                btnShuffle.setImageResource(R.drawable.ic_shuffle_on);
            } else {
                //Setting repeat mode off and replacing icon
                SongQueue.getInstance().setShuffle_on(false);
                btnShuffle.setImageResource(R.drawable.ic_shuffle);
            }
        });
    }

    //This function sets up and implements a live rewind seekbar
    public void set_up_bar() {
        if (player == null) {
            ;
        } else {
            startSeekBarUpdate();
            //If player is already ready then, initialize differently for bottom navigation bar opening
            if (player.getPlaybackState() == Player.STATE_READY) {
                long duration = player.getDuration();
                if (formatTime(duration) == textTotalDuration.getText()) {
                    ;
                } else {
                    textTotalDuration.setText(formatTime(duration));
                }
                seekBar.setMax((int) duration);
            } else {
                // Handle unknown duration case, possibly set to live stream duration handling
                textTotalDuration.setText("0:00");
            }
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        player.seekTo(progress);
                        SongQueue.getInstance().setCurrent_time(player.getCurrentPosition());
                        last_position = player.getCurrentPosition();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    stopSeekBarUpdate();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    startSeekBarUpdate();
                }
            });
        }
    }
    //This function updates the seekbar based on the duration of song
    private void startSeekBarUpdate() {
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (player != null && player.isPlaying()) {
                        seekBar.setProgress((int) player.getCurrentPosition());
                        textCurrentTime.setText(formatTime(player.getCurrentPosition()));
                        SongQueue.getInstance().setCurrent_time(player.getCurrentPosition());
                        //Handling song finished functionality
                        handler.postDelayed(this, 1000);
                    }
                } catch (Exception e) {
                    ;
                }
            }
        };
        handler.post(runnable);
    }

    //This function stops updating seekbar
    private void stopSeekBarUpdate() {
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }

    //This function formats string is data and time format 0:00
    private String formatTime(long timeMs) {
        int totalSeconds = (int) (timeMs / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    //This function plays the specified music file
    private void set_up_view(MusicFile musicFile) {

        player = OfflinePlayerManager.getInstance().current_player;
        //Initializing song properties
        session_id = SongQueue.getInstance().getAudio_session_id();
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
        //Initialising reverb settings
        reverb = SongQueue.getInstance().reverb;
        song_speed = SongQueue.getInstance().speed;
        song_pitch = SongQueue.getInstance().pitch;
        reverb_level = SongQueue.getInstance().reverb_level;
        //Setting playback speed properties
        //Setting up speed+pitch seekbar
        seekBarSpeed.setMax(200);
        seekBarSpeed.setMin(50);
        seekBarSpeed.setProgress((int) (song_speed * 100));
        speed_text.setText(String.format("%.1fx", song_speed));
        //Setting reverb bar to lowest
        seekBarReverb.setMax(2000);
        seekBarReverb.setMin(0);
        seekBarReverb.setProgress(reverb_level);
        setReverbPreset(reverb_level);
    }


    //This function loads User music audio files from personal directory
    //This function loads User music audio files from personal directory
    private void loadMusicFiles() {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = getContext().getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                null
        )) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                //Getting music information
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                String artist = cursor.getString(artistColumn);
                String data = cursor.getString(dataColumn);
                long albumId = cursor.getLong(albumIdColumn);
                //Defining music file
                MusicFile musicFile = new MusicFile(id, name, artist, data, albumId);
                //Filtering out music from short sounds and voice recordings
                if (artist.equals("Voice Recorder")) {
                    ;
                } else if (artist.equals("<unknown>")) {
                    ;
                } else {
                    musicFiles.add(musicFile);
                }
            }
        }
        if (SongQueue.getInstance().current_resource == R.layout.item_song2) {
            musicFiles = SongQueue.getInstance().song_list;
        } else {
            ;
        }

    }
    //This function sets up speed manager seek bar
    public void set_up_speed_and_pitch() {
        // SeekBar for Speed
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Setting speed between 0.5x and 2.0x
                float speed = Math.max(0.5f, Math.min(progress / 100f, 2.0f));
                song_pitch = speed;
                song_speed = speed;
                speed_text.setText(String.format("%.1fx", speed)); // Update the speed text
                if (player != null) {
                    player.setPlaybackParameters(new PlaybackParameters(song_speed, song_pitch));
                }
                seekBarSpeed.setProgress(progress);
                SongQueue.getInstance().setSpeed(song_speed);
                SongQueue.getInstance().setPitch(song_pitch);
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
            reverb_level = progress;
            if (progress <= -1000) {
                progress = 0;
            }
            reverb_text.setText("Reverberation: " + (progress-1000) + "db");
            seekBar.setProgress(progress);
            SongQueue.getInstance().setReverb_level(reverb_level);
            if (playerService != null) {
                playerService.setReverbPreset(progress);
            } else {
                ;
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayerService();
        if (runnable != null && handler != null) {
            handler.removeCallbacks(runnable);
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