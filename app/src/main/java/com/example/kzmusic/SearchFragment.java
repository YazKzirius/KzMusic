package com.example.kzmusic;

//Imports
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.inputmethod.EditorInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
//This class implements the search page
public class SearchFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    MusicAdapter musicAdapter;
    SpotifyAppRemote mSpotifyAppRemote;
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String REDIRECT_URI = "kzmusic://callback";
    private List<SearchResponse.Track> trackList = new ArrayList<>();
    String accesstoken;
    PlayerApi player;
    View view;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private static final String CHANNEL_ID = "media_playback_channel3";
    private static final int NOTIFICATION_ID = 3;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    ExoPlayer exo_player;
    private EnvironmentalReverb reverb;
    int session_id;
    private SharedViewModel sharedViewModel;

    public SearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SearchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
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
        view =  inflater.inflate(R.layout.fragment_search, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.down_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view3);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (getArguments() != null) {
            accesstoken = getArguments().getString("Token");
        }
        musicAdapter = new MusicAdapter(trackList, getContext(), new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchResponse.Track track) {
                //Pausing current player, so no playback overlap
                if (PlayerManager.getInstance().get_size() > 0) {
                    PlayerManager.getInstance().current_player.pause();
                    SpotifyPlayerLife.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    SpotifyPlayerLife.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                }
            }
        });
        recyclerView.setAdapter(musicAdapter);
        TextView View = view.findViewById(R.id.results);
        EditText search = view.findViewById(R.id.search_input);
        View.setText("Search results:");
        display_random(accesstoken);
        Button search_button = view.findViewById(R.id.search_button);
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Searching for random tracks based on the name input
                String input = search.getText().toString();
                if (input.equals("")) {
                    display_random(accesstoken);
                    View.setText("Search results:");
                } else {
                    //Displaying results
                    search_track(input, accesstoken);
                    View.setText("Search results:");
                    display_random(accesstoken);
                }
            }
        });
        //Implementing functionality for enter button clicking
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    // Handle the Enter key event here
                    String input = search.getText().toString();
                    //Displaying search results
                    if (input.equals("")) {
                        display_random(accesstoken);
                    } else {
                        search_track(input, accesstoken);
                    }
                    View.setText("Search results:");
                    return true; // Return true to indicate the event was handled
                }
                return false; // Return false if the event is not handled
            }
        });
        //Setting up bottom playback navigator
        set_up_spotify_play();
        set_up_play_bar();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ;
    }
    //This function sets up media notification bar skip events
    public void set_up_skipping() {
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        SharedViewModelProvider.initViewModel(this);  // Initialize the ViewModelProvider

        sharedViewModel.getSkipEvent().observe(getViewLifecycleOwner(), skip -> {
            set_up_play_bar();
        });
    }
    //This function plays the specified music file
    private void playMusic(MusicFile musicFile) {
        //Playing resuming song at previous duration if the same song as last
        if (SongQueue.getInstance().get_size() > 1) {
            int index = SongQueue.getInstance().pointer - 1;
            //Getting current and previous song names
            String s1 = SongQueue.getInstance().get_specified(index).getName();
            String s2 = SongQueue.getInstance().get_specified(index - 1).getName();
            if (s1.equals(s2)) {
                //Resuming at left point
                //Use previous player
                exo_player = PlayerManager.getInstance().current_player;
            } else {
                PlayerManager.getInstance().stopAllPlayers();
                exo_player = new ExoPlayer.Builder(getContext()).build();
                Uri uri = Uri.fromFile(new File(musicFile.getPath()));
                MediaItem mediaItem = MediaItem.fromUri(uri);
                exo_player.setMediaItem(mediaItem);
            }
        } else {
            exo_player = new ExoPlayer.Builder(getContext()).build();
            Uri uri = Uri.fromFile(new File(musicFile.getPath()));
            MediaItem mediaItem = MediaItem.fromUri(uri);
            exo_player.setMediaItem(mediaItem);
        }
        //Initializing song properties
        session_id = exo_player.getAudioSessionId();
        //Applying audio effects
        apply_audio_effect();
        exo_player.prepare();
        exo_player.play();
        //Adds player to Player session manager
        PlayerManager.getInstance().addPlayer(exo_player);
        PlayerManager.getInstance().setCurrent_player(exo_player);
    }
    //This function assigns audio effects to the exoplayer like speed/reverb
    public void apply_audio_effect() {
        //Initialising reverb settings
        SongQueue.getInstance().initialize_reverb(session_id);
        reverb = SongQueue.getInstance().reverb;
        //Setting playback speed properties
        exo_player.setPlaybackParameters(new PlaybackParameters(SongQueue.getInstance().speed, SongQueue.getInstance().pitch));
        //Setting reverberation properties
        setReverbPreset(SongQueue.getInstance().reverb_level);
    }
    //This function sets reverb level based on seekbar progress level
    private void setReverbPreset(int progress) {
        //Computing reverberation parameters based of reverb level data proportionality
        try {
            int room_level = -2000 + (progress + 1000);
            double decay_level = 10000;
            reverb.setReverbLevel((short) progress);
            reverb.setDecayTime((int) decay_level);
            reverb.setRoomLevel((short) room_level);
            reverb.setEnabled(true);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Something went wrong with audio effects", Toast.LENGTH_SHORT).show();
        }
    }
    //This function opens Spotify player overlay
    public void open_spotify_overlay() {
        Fragment spotify_overlay = new SpotifyOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, spotify_overlay);
        fragmentTransaction.commit();
    }
    //This function makes an API call using previous access token to search for random music
    //It does this based on the track_name entered
    private void search_track(String track_name, String token) {
        accesstoken = token;
        String randomQuery = track_name;
        SpotifyApiService apiService = RetrofitClient.getClient(accesstoken).create(SpotifyApiService.class);
        Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    trackList.clear();
                    trackList.addAll(response.body().getTracks().getItems());
                    musicAdapter.notifyDataSetChanged();
                } else {
                    Intent intent = new Intent(getContext(), GetStarted.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                TextView text1 = view.findViewById(R.id.results);
                text1.setText("No internet connection, please try again.");
            }
        });
    }
    //This function gets random music based on catergory
    //It does this before the user chooses to search for a random track
    private void display_random(String token) {
        accesstoken = token;
        String[] randomQueries = {"happy", "sad", "party", "chill", "love", "workout"};
        String randomQuery = randomQueries[(int) (Math.random() * randomQueries.length)];
        SpotifyApiService apiService = RetrofitClient.getClient(accesstoken).create(SpotifyApiService.class);
        Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    trackList.clear();
                    trackList.addAll(response.body().getTracks().getItems());
                    musicAdapter.notifyDataSetChanged();
                } else {
                    Intent intent = new Intent(getContext(), GetStarted.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                TextView text1 = view.findViewById(R.id.results);
                text1.setText("No internet connection, please try again.");
            }
        });
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
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(SongQueue.getInstance().current_song.getAlbumId()));
        //Once the resource loads, it changes to that background picture
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
        Glide.with(getContext()).asBitmap().load(album_uri).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                builder.setLargeIcon(resource);
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

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                ;
            }
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                // Handle the load failure, you can use a default or error bitmap here
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
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
        });
    }
    //This function creates a new media session for specific player
    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(getContext(), "ExoPlayerMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Random rand = new Random();
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, SongQueue.getInstance().speed);
        mediaSession.setPlaybackState(stateBuilder.build());
        PlayerManager.getInstance().addSession(mediaSession);
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
                        playMusic(song);
                        updateNotification(song);
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
                        playMusic(song);
                        updateNotification(song);
                    }
                }
            }
        });
        mediaSession.setActive(true);
    }
    //This function updates the current notification view holder when a song is skipped
    private void updateNotification(MusicFile musicFile) {
        //Updating current notification with new details and meta data
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.library)
                .setContentTitle(format_title(musicFile.getName()))
                .setContentText(musicFile.getArtist().replaceAll("/", ", "))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0));
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
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