package com.example.kzmusic;

//Imports
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaMetadataRetriever;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.media.session.MediaButtonReceiver;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserMusic#newInstance} factory method to
 * create an instance of this fragment.
 */
//This class manages the user music page
public class UserMusic extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private static final int REQUEST_CODE = 1;
    private List<MusicFile> musicFiles = new ArrayList<>();
    private RecyclerView recyclerView;
    private MusicFileAdapter musicAdapter;
    View view;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private static final String CHANNEL_ID = "media_playback_channel4";
    private static final int NOTIFICATION_ID = 4;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private SharedViewModel sharedViewModel;

    public UserMusic() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserMusic.
     */
    // TODO: Rename and change types and number of parameters
    public static UserMusic newInstance(String param1, String param2) {
        UserMusic fragment = new UserMusic();
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
        view = inflater.inflate(R.layout.fragment_user_music, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.down_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        recyclerView = view.findViewById(R.id.recycler_view2);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicFileAdapter(getContext(), musicFiles);
        recyclerView.setAdapter(musicAdapter);
        //Checks for manifest external storage permissions
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_CODE);
        } else {
            //Loading music files into recycler view
            loadMusicFiles();
            SongQueue.getInstance().setSong_list(musicFiles);
        }
        musicAdapter.notifyDataSetChanged();
        //Setting up bottom playback navigator
        set_up_spotify_play();
        set_up_play_bar();
        return view;
    }
    //This function sets up media notification bar skip events
    public void set_up_skipping() {
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        SharedViewModelProvider.initViewModel(this);  // Initialize the ViewModelProvider

        sharedViewModel.getSkipEvent().observe(getViewLifecycleOwner(), skip -> {
            set_up_play_bar();
        });
    }
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
    //This function assigns data from playback overlay to bottom navigation
    public void set_up_play_bar() {
        if (SongQueue.getInstance().songs_played.size() == 0) {
            ;
        } else {
            MusicFile song = SongQueue.getInstance().current_song;
            int pos = SongQueue.getInstance().current_position;
            design_bar();
            //When bottom song navigator is clicked, relocate back to playback overlay
            Artist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            art.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            playback_bar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            //Implementing down button functionality
            ic_down.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Opening song overay
                    open_new_overlay(song, pos);}
            });
        }
    }
    //This function designs the bottom playback bar
    public void design_bar() {
        MusicFile song = SongQueue.getInstance().current_song;
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(song.getAlbumId()));
        Glide.with(getContext()).asBitmap().load(album_uri).circleCrop().into(art);
        title.setText("Now playing "+format_title(song.getName()));
        Artist.setText(song.getArtist().replaceAll("/", ", "));
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
        String prefix = title.charAt(0)+""+title.charAt(1)+""+title.charAt(2);
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
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
    }
    //This function handles Spotify overlay play/pause
    public void set_up_spotify_play() {
        if (SpotifyPlayerLife.getInstance().mSpotifyAppRemote != null) {
            SpotifyPlayerLife.getInstance().mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {
                    if (playerState.isPaused) {
                        ;
                    } else {
                        if (PlayerManager.getInstance().current_player != null) {
                            PlayerManager.getInstance().current_player.pause();
                        } else {
                            ;
                        }
                    }
                }
            });
        }
    }
    //This function creates the media playback notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media playback",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Media playback controls");

            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //This function creates the playback controls for notification channel
    private void showNotification(PlaybackStateCompat state) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID);
        builder.setContentTitle(format_title(SongQueue.getInstance().current_song.getName()))
                .setContentText(SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", "))
                .setSmallIcon(R.drawable.library)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0));
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(
                    getContext(), PlaybackStateCompat.ACTION_PAUSE)));
        } else {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(
                    getContext(), PlaybackStateCompat.ACTION_PLAY)));
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    //This function creates a new media session for specific player
    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(getContext(), "ExoPlayerMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Random rand = new Random();
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, SongQueue.getInstance().speed);
        mediaSession.setPlaybackState(stateBuilder.build());
        showNotification(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                // Update playback state to playing
                PlayerManager.getInstance().current_player.play();
                stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f);
                mediaSession.setPlaybackState(stateBuilder.build());
                showNotification(stateBuilder.build());
            }

            @Override
            public void onPause() {
                super.onPause();
                //Update playback state to paused
                PlayerManager.getInstance().current_player.pause();
                stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f);
                mediaSession.setPlaybackState(stateBuilder.build());
                showNotification(stateBuilder.build());
            }
        });
        mediaSession.setActive(true);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.getController().getTransportControls().stop();
            mediaSession.release();
        }
    }
}
