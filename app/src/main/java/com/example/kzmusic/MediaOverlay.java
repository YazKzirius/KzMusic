package com.example.kzmusic;
//Imports
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.net.Uri;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;


import android.media.audiofx.PresetReverb;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;
import android.widget.Toast;

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
    Boolean is_looping;
    private ImageButton btnLoop;
    private ImageButton btnSkip_left;
    private ImageButton btnSkip_right;
    private SeekBar seekBar;
    private SimpleExoPlayer player;
    private TextView speed_text;
    private TextView pitch_text;
    private PresetReverb reverb;
    private TextView textCurrentTime, textTotalDuration;
    private CircularImageViewWithBeatTracker imageViewWithBeatTracker;
    private Runnable beatRunnable;
    private Runnable runnable;
    private float[] beatLevels = new float[150];
    float song_speed;
    float song_pitch;
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
        //Implementing player functionality
        view = inflater.inflate(R.layout.fragment_media_overlay, container, false);
        overlaySongTitle = view.findViewById(R.id.songTitle);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnLoop = view.findViewById(R.id.btnLoop);
        btnSkip_left = view.findViewById(R.id.btnSkipLeft);
        btnSkip_right = view.findViewById(R.id.btnSkipRight);
        speed_text = view.findViewById(R.id.speed_text);
        pitch_text = view.findViewById(R.id.pitch_text);
        seekBar = view.findViewById(R.id.seekBar);
        textCurrentTime = view.findViewById(R.id.textCurrentTime);
        textTotalDuration = view.findViewById(R.id.textTotalDuration);
        if (getArguments() != null) {
            musicFile = getArguments().getParcelable("song");
            position = getArguments().getInt("position");
            is_looping = getArguments().getBoolean("is_looping");
            if (musicFile != null) {
                player = new SimpleExoPlayer.Builder(getContext()).build();
                // Set song details
                //This function sets up the circular view for a song with no album art
                set_up_circular_view_blank(R.drawable.ic_library);
                playMusic(musicFile);
                //Setting up circular view with beats around for song with album art
                set_up_circular_view(musicFile);
                //Loading previous music files
                loadMusicFiles();
                //Setting up media buttons
                set_up_media_buttons();
                //Setting up seekbar
                set_up_bar();
                set_up_speed();
                set_up_pitch();
            }
        }
        return view;
    }
    //This function sets up music image view
    public void set_up_circular_view(MusicFile file) {
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(file.getAlbumId()));
        imageViewWithBeatTracker = view.findViewById(R.id.musicImage);
        // Load image using Glide
        imageViewWithBeatTracker.loadImage(album_uri);
        // Example: Update beat levels every 500ms
        beatRunnable = new Runnable() {
            @Override
            public void run() {
                // Generate random beat levels for demonstration
                for (int i = 0; i < beatLevels.length; i++) {
                    beatLevels[i] = (float) Math.random();
                }
                imageViewWithBeatTracker.updateBeatLevels(beatLevels);
                handler.postDelayed(this, 100);
            }
        };
        handler.post(beatRunnable);
    }
    //This function sets up circular view with beats for blank album art
    public void set_up_circular_view_blank(int Id) {
        // Load album image
        imageViewWithBeatTracker = view.findViewById(R.id.musicImage);
        // Load image using Glide
        imageViewWithBeatTracker.loadImageResource(Id);
        // Example: Update beat levels every 500ms
        beatRunnable = new Runnable() {
            @Override
            public void run() {
                // Generate random beat levels for demonstration
                for (int i = 0; i < beatLevels.length; i++) {
                    beatLevels[i] = (float) Math.random();
                }
                imageViewWithBeatTracker.updateBeatLevels(beatLevels);
                handler.postDelayed(this, 100);
            }
        };
        handler.post(beatRunnable);
    }
    //This function sets up and implements button functionality
    public void set_up_media_buttons() {
        //Pause/play functionality
        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            }
        });
        //Loop functionality
        //If loop was on previously, keep loop on otherwise, continue
        if (is_looping == true) {
            //Setting repeat mode on and replacing icon
            player.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_ONE);
            btnLoop.setImageResource(R.drawable.ic_loop_on);
        } else {
            //Setting repeat mode off and replacing icon
            player.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_OFF);
            btnLoop.setImageResource(R.drawable.ic_loop);
        }
        //Loop button click functionality
        btnLoop.setOnClickListener(v -> {
            is_looping = !is_looping;
            if (is_looping == true) {
                player.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_ONE);
                btnLoop.setImageResource(R.drawable.ic_loop_on);
            } else {
                player.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_OFF);
                btnLoop.setImageResource(R.drawable.ic_loop);
            }
        });
        //Skip button functionality
        btnSkip_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                position -= 1;
                musicFile = musicFiles.get(position);
                open_new_overlay(musicFile, position);

            }
        });
        btnSkip_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                position += 1;
                musicFile = musicFiles.get(position);
                open_new_overlay(musicFile, position);
            }
        });
    }
    //This function sets up and implements a live rewind seekbar
    public void set_up_bar() {
        //Seekbar functionality
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    startSeekBarUpdate();
                } else {
                    stopSeekBarUpdate();
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long duration = player.getDuration();
                    textTotalDuration.setText(formatTime(duration));
                    seekBar.setMax((int) duration);
                } else {
                    // Handle unknown duration case, possibly set to live stream duration handling
                    textTotalDuration.setText("0:00");
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
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
    private void playMusic(MusicFile musicFile) {
        //Stops all players before playing new song
        ExoPlayerManager.getInstance().stopAllPlayers();
        Uri uri = Uri.fromFile(new File(musicFile.getPath()));
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        //Adds player to Player session manager
        ExoPlayerManager.getInstance().addPlayer(player);
        overlaySongTitle.setText(musicFile.getName()+" by "+musicFile.getArtist());
    }
    //This function updates seek bar duration based on speed changer
    private void updateSeekBarDuration() {
        if (player != null) {
            long duration = player.getDuration();
            float playbackSpeed = player.getPlaybackParameters().speed;
            long adjustedDuration = (long) (duration / playbackSpeed);
            seekBar.setMax((int) adjustedDuration);
            textTotalDuration.setText(formatTime(adjustedDuration));
        }
    }
    //This function updates the seekbar based on the duration of song
    private void startSeekBarUpdate() {
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    seekBar.setProgress((int) player.getCurrentPosition());
                    textCurrentTime.setText(formatTime(player.getCurrentPosition()));
                    handler.postDelayed(this, 1000);
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
    }
    //This function opens a new overlay
    //This function opens the playback handling overlay
    public void open_new_overlay(MusicFile musicFile, int position) {
        if (reverb != null) {
            reverb.release();
        }
        Fragment media_page = new MediaOverlay();
        Bundle bundle = new Bundle();
        bundle.putParcelable("song", musicFile);
        bundle.putInt("position", position);
        bundle.putBoolean("is_looping", is_looping);
        media_page.setArguments(bundle);
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    //This function sets up speed manager seek bar
    public void set_up_speed() {
        // SeekBar for Speed
        SeekBar seekBarSpeed = view.findViewById(R.id.seekBarSpeed);
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Setting speed between 0.5x and 2.0x
                float speed = Math.max(0.5f, Math.min(progress / 100f, 2.0f));
                speed_text.setText(String.format("Speed: %.1fx", speed)); // Update the speed text
                player.setPlaybackParameters(new PlaybackParameters(speed, player.getPlaybackParameters().pitch));
                //Updating seekbar duration
                updateSeekBarDuration();
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
    public void set_up_pitch() {
        // SeekBar for Pitch
        SeekBar seekBarPitch = view.findViewById(R.id.seekBarPitch);
        seekBarPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Setting pitch between 0.5x and 2.0x
                float pitch = Math.max(0.5f, Math.min(progress / 100f, 2.0f));
                pitch_text.setText(String.format("Pitch: %.1fx", pitch)); // Update the pitch text
                player.setPlaybackParameters(new PlaybackParameters(player.getPlaybackParameters().speed, pitch));

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (reverb != null) {
            reverb.release();
        }
    }
}