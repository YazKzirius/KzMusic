package com.example.kzmusic;
//Imports
import java.util.Random;
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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.net.Uri;
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
    Boolean is_black_image = false;
    private Runnable beatRunnable;
    private Runnable runnable;
    private float[] beatLevels = new float[150];
    float song_speed = (float) 1.0;
    float song_pitch = (float) 1.0;
    int reverb_level = -1000;
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
        if (SpotifyPlayerLife.getInstance().mSpotifyAppRemote != null) {
            SpotifyPlayerLife.getInstance().pause_playback();
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
        song_speed = SongQueue.getInstance().speed;
        song_pitch = SongQueue.getInstance().pitch;
        reverb_level = SongQueue.getInstance().reverb_level;
        //Playing music
        playMusic(musicFile);
        //Setting up circular view with beats around for song with album art
        set_up_circular_view(musicFile);
        //Loading previous music files
        loadMusicFiles();
        //Setting up media buttons
        set_up_media_buttons();
        //Setting up seekbar
        set_up_bar();
        //Setting up speed+pitch seekbar functionality
        set_up_speed_and_pitch();
        //Setting up reverberation seekbar functionality
        set_up_reverb();
        //Setting up spotify play buttons
        return view;
    }
    //This function sets up music image view
    public void set_up_circular_view(MusicFile file) {
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(file.getAlbumId()));
        Glide.with(getContext()).asBitmap().load(album_uri).circleCrop().into(album_cover);
        Glide.with(getContext()).asGif().load(R.drawable.media_playing).circleCrop().into(song_gif);
    }

    //This function sets up and implements button functionality
    public void set_up_media_buttons() {
        //Pause/play functionality
        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                // Stop the GIF by clearing the ImageView
                Glide.with(getContext()).clear(song_gif);
                song_gif.setImageDrawable(null);
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                set_up_circular_view(musicFile);
            }
        });
        //Loop functionality
        //If loop was on previously, keep loop on otherwise, continue
        if (is_looping == true) {
            //Setting repeat mode on and replacing icon
            player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
            SongQueue.getInstance().setIs_looping(true);
            btnLoop.setImageResource(R.drawable.ic_loop_on);
        } else {
            //Setting repeat mode off and replacing icon
            player.setRepeatMode(ExoPlayer.REPEAT_MODE_OFF);
            SongQueue.getInstance().setIs_looping(false);
            btnLoop.setImageResource(R.drawable.ic_loop);
        }
        //Loop button click functionality
        btnLoop.setOnClickListener(v -> {
            is_looping = !is_looping;
            if (is_looping == true) {
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                SongQueue.getInstance().setIs_looping(true);
                btnLoop.setImageResource(R.drawable.ic_loop_on);
            } else {
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_OFF);
                SongQueue.getInstance().setIs_looping(false);
                btnLoop.setImageResource(R.drawable.ic_loop);
            }
        });
        //Skip button functionality
        Random rand = new Random();
        btnSkip_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                //Moving to next song in recycler view if shuffle is off
                if (shuffle_on == false) {
                    //Handling the event that current song is top of recycler view
                    if (position == 0) {
                        ;
                    } else {
                        position -= 1;
                    }
                } else {
                    position = rand.nextInt(musicFiles.size());
                }
                musicFile = musicFiles.get(position);
                open_new_overlay();
            }
        });
        btnSkip_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                //Moving to next song in recycler view if shuffle is off
                if (shuffle_on == false) {
                    //Handling the event that it's the last song in the recycler view
                    if (position == musicFiles.size()-1) {
                        ;
                    } else {
                        position += 1;
                    }
                } else {
                    position = rand.nextInt(musicFiles.size());
                }
                musicFile = musicFiles.get(position);
                open_new_overlay();
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
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
                    SongQueue.getInstance().setCurrent_time(player.getCurrentPosition());
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
        PlayerManager.getInstance().stopAllPlayers();
        Uri uri = Uri.fromFile(new File(musicFile.getPath()));
        MediaItem mediaItem = MediaItem.fromUri(uri);
        // Set song details
        player = new ExoPlayer.Builder(getContext()).build();
        session_id = player.getAudioSessionId();
        //Initializing reverb from Song manager class
        SongQueue.getInstance().initialize_reverb(session_id);
        reverb = SongQueue.getInstance().reverb;
        String display_title = musicFile.getName().replace("[SPOTIFY-DOWNLOADER.COM] ", "").replace(".mp3", "")+" by "+musicFile.getArtist();
        player.setMediaItem(mediaItem);
        player.prepare();
        //Adds player to Player session manager
        PlayerManager.getInstance().addPlayer(player);
        PlayerManager.getInstance().setCurrent_player(player);
        overlaySongTitle.setText(display_title);
        //Applying audio effects
        apply_audio_effect();
        player.play();
        //Playing resuming song at previous duration if the same song as last
        if (SongQueue.getInstance().get_size() > 1) {
            int index = SongQueue.getInstance().pointer- 1;
            //Getting current and previous song names
            String s1 = SongQueue.getInstance().get_specified(index).getName();
            String s2 = SongQueue.getInstance().get_specified(index-1).getName();
            if (s1.equals(s2)) {
                //Resuming at left point
                player.seekTo(SongQueue.getInstance().current_time+500);
                seekBar.setProgress((int) SongQueue.getInstance().current_time+500);
            }
        }
    }
    //This function assigns audio effects to the exoplayer like speed/reverb
    public void apply_audio_effect() {
        //Setting playback speed properties
        player.setPlaybackParameters(new PlaybackParameters(song_speed, song_pitch));
        speed_text.setText(String.format("Speed + pitch: %.1fx", song_speed));
        //Setting up speed+pitch seekbar
        seekBarSpeed.setMax(200);
        seekBarSpeed.setMin(50);
        seekBarSpeed.setProgress((int)(song_speed*100));
        //Setting reverberation properties
        setReverbPreset(reverb_level);
        //Setting reverb bar to lowest
        seekBarReverb.setMax(1000);
        seekBarReverb.setMin(-1000);
        seekBarReverb.setProgress(reverb_level);
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
                    SongQueue.getInstance().setCurrent_time(player.getCurrentPosition());
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
    public void open_new_overlay() {
        //Resetting if not null
        if (reverb != null) {
            reverb.release();
        }
        //Adding new song to queue
        SongQueue.getInstance().addSong(musicFile);
        SongQueue.getInstance().setPosition(position);
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
                speed_text.setText(String.format("Speed + pitch: %.1fx", speed)); // Update the speed text
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
        int room_level = -2000 + (progress + 1000);
        double decay_level = 10000;
        reverb.setReverbLevel((short) progress);
        reverb.setDecayTime((int) decay_level);
        reverb.setRoomLevel((short) room_level);
        //Estimating percentage of seekbar complete
        double percentage = ((double) (progress+1000) / 2000) * 100;
        reverb.setEnabled(true);
        reverb_level = progress;
        reverb_text.setText("Reverberation: "+(int) percentage / 2+"%");
        seekBar.setProgress(progress);
        SongQueue.getInstance().setReverb_level(reverb_level);
    }
}