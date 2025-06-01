package com.example.kzmusic;
//Imports
import java.util.Random;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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

import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        return view;
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
                        Glide.with(getContext())
                                .asBitmap()
                                .load(R.drawable.logo) // Backup image resource
                                .circleCrop()
                                .into(album_cover);
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
                    // Handle the skip event in the fragment
                    musicFile = SongQueue.getInstance().current_song;
                    player = OfflinePlayerManager.getInstance().current_player;
                    //Initializing reverb from Song manager class
                    String display_title = format_title(musicFile.getName()) + " by " + musicFile.getArtist().replaceAll("/", ", ");
                    overlaySongTitle.setText(display_title);
                    //Displaying circular view
                    set_up_circular_view(musicFile);
                    //Setting up seekbar
                    set_up_bar();
                } else {
                    ;
                }
            }
        });
        //Setting up for Pause events
        sharedViewModel.getPauseEvent().observe(getViewLifecycleOwner(), event ->  {
            if (event != null)  {
                Boolean shouldPlayPause = event.getContentIfNotHandled();
                if (shouldPlayPause != null && shouldPlayPause) {
                    player = OfflinePlayerManager.getInstance().current_player;
                    // Stop the GIF by clearing the ImageView
                    Glide.with(getContext()).clear(song_gif);
                    song_gif.setImageDrawable(null);
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    ;
                }
            }
        });
        //Setting up for Play events
        sharedViewModel.getPlayEvent().observe(getViewLifecycleOwner(), event ->  {
            if (event != null)  {
                Boolean shouldPlayPause = event.getContentIfNotHandled();
                if (shouldPlayPause != null && shouldPlayPause) {
                    player = OfflinePlayerManager.getInstance().current_player;
                    //Update duration in database
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    set_up_circular_view(SongQueue.getInstance().current_song);
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
                    musicFile = SongQueue.getInstance().current_song;
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
    //This function updates the total duration value
    public void update_total_duration() {
        long currentPosition = OfflinePlayerManager.getInstance().current_player.getCurrentPosition();
        long duration = currentPosition - last_position;
        Toast.makeText(getContext(), ""+duration, Toast.LENGTH_LONG).show();
        // 🔥 Prevent negative duration
        if (duration < 0) {
            Log.e("ExoPlayer", "Negative duration detected! Resetting to 0.");
            duration = 0;
        }
        SongQueue.getInstance().update_duration((int) (duration / (1000 * SongQueue.getInstance().speed)));
        // ✅ Update last position safely
        last_position = currentPosition;
        SongQueue.getInstance().setLast_postion(last_position);
    }

    //This function sets up and implements button functionality
    public void set_up_media_buttons() {
        //Pause/play functionality
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPlaying()) {
                    player.pause();
                    //Update duration in database
                    update_total_duration();
                    // Stop the GIF by clearing the ImageView
                    Glide.with(getContext()).clear(song_gif);
                    song_gif.setImageDrawable(null);
                    //Updating service state
                    playerService.updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    playerService.showNotification(playerService.stateBuilder.build());
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    player.play();
                    //Updating service state
                    playerService.updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    playerService.showNotification(playerService.stateBuilder.build());
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    set_up_circular_view(musicFile);
                }
            }
        });
        //Loop functionality
        //If loop was on previously, keep loop on otherwise, continue
        if (is_looping == true) {
            //Setting repeat mode on and replacing icon
            SongQueue.getInstance().setIs_looping(true);
            playerService.updatePlaybackState(PlaybackStateCompat.REPEAT_MODE_ONE);
            playerService.updateNotification(musicFile);
            btnLoop.setImageResource(R.drawable.ic_loop_on);
        } else {
            //Setting repeat mode off and replacing icon
            SongQueue.getInstance().setIs_looping(false);
            btnLoop.setImageResource(R.drawable.ic_loop);
        }
        //Loop button click functionality
        btnLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Loop functionality
                is_looping = !is_looping;
                //If loop was on previously, keep loop on otherwise, continue
                if (is_looping == true) {
                    //Setting repeat mode on and replacing icon
                    playerService.updatePlaybackState(PlaybackStateCompat.REPEAT_MODE_ONE);
                    playerService.updateNotification(musicFile);
                    SongQueue.getInstance().setIs_looping(true);
                    btnLoop.setImageResource(R.drawable.ic_loop_on);
                } else {
                    //Setting repeat mode off and replacing icon
                    SongQueue.getInstance().setIs_looping(false);
                    btnLoop.setImageResource(R.drawable.ic_loop);
                }
            }
        });
        //Skip button functionality
        Random rand = new Random();
        btnSkip_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                //Updating total song duration in database
                update_total_duration();
                SessionManager sessionManager = new SessionManager(getContext());
                String email = sessionManager.getEmail();
                SongsFirestore table = new SongsFirestore(getContext());
                String display_title = format_title(SongQueue.getInstance().current_song.getName()) + " by " + SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", ");
                table.updateTotalDuration(email, display_title, SongQueue.getInstance().duration_played);
                SongQueue.getInstance().setDuration_played(0);
                //Moving to next song in recycler view if shuffle is off
                if (shuffle_on == false) {
                    //Handling the event that current song is top of recycler view
                    if (position == 0) {
                        position = musicFiles.size() - 1;
                    } else {
                        position -= 1;
                    }
                } else {
                    position = rand.nextInt(musicFiles.size());
                }
                musicFile = musicFiles.get(position);
                //Adding new song to queue
                SongQueue.getInstance().addSong(musicFile);
                SongQueue.getInstance().setPosition(position);
                playerService.updateNotification(musicFile);
                playerService.playMusic(musicFile);
                set_up_view(musicFile);
            }
        });
        btnSkip_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                update_total_duration();
                SessionManager sessionManager = new SessionManager(getContext());
                String email = sessionManager.getEmail();
                SongsFirestore table = new SongsFirestore(getContext());
                String display_title = format_title(SongQueue.getInstance().current_song.getName()) + " by " + SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", ");
                table.updateTotalDuration(email, display_title, SongQueue.getInstance().duration_played);
                SongQueue.getInstance().setDuration_played(0);
                //Moving to next song in recycler view if shuffle is off
                if (shuffle_on == false) {
                    //Handling the event that it's the last song in the recycler view
                    if (position == musicFiles.size() - 1) {
                        position = 0;
                    } else {
                        position += 1;
                    }
                } else {
                    position = rand.nextInt(musicFiles.size());
                }
                musicFile = musicFiles.get(position);
                //Updating total song duration in database
                //Adding new song to queue
                SongQueue.getInstance().addSong(musicFile);
                SongQueue.getInstance().setPosition(position);
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
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

    }

    //This function sets up and implements a live rewind seekbar
    public void set_up_bar() {
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

    //This function plays the specified music file
    private void set_up_view(MusicFile musicFile) {;
        player = OfflinePlayerManager.getInstance().current_player;
        //Initializing song properties
        session_id = SongQueue.getInstance().getAudio_session_id();
        //Initializing reverb from Song manager class
        String display_title = format_title(musicFile.getName()) + " by " + musicFile.getArtist().replaceAll("/", ", ");
        //Applying audio effects
        apply_audio_effect();
        overlaySongTitle.setText(display_title);
        //Displaying circular view
        set_up_circular_view(musicFile);
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
        //Checking if title ends with empty space
        if (title.endsWith(" ")) {
            title = title.substring(0, title.lastIndexOf(" "));
        }
        //Checking if prefix is at the start and if it occurs again
        if (isOnlyDigits(prefix) && title.indexOf(prefix) == 0 && title.indexOf(prefix, 2) == -1) {
            //Removing prefix
            title = title.replaceFirst(prefix, "");
        } else {
            ;
        }
        return title;
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
        seekBarReverb.setMax(1000);
        seekBarReverb.setMin(-1000);
        seekBarReverb.setProgress(reverb_level);
        setReverbPreset(reverb_level);
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

    //This function opens a new overlay
    //This function opens the playback handling overlay
    public void open_new_overlay() {
        Fragment media_page = new MediaOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
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
                player.setPlaybackParameters(new PlaybackParameters(song_speed, song_pitch));
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
            double percentage = ((double) (progress + 1000) / 2000) * 100;
            reverb_level = progress;
            reverb_text.setText("Reverberation: " + (int) percentage / 2 + "%");
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