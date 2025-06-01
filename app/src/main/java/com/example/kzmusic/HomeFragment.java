package com.example.kzmusic;

//Imports
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
//This implements the default Homepage fragment
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    View view;
    SessionManager sessionManager;
    String email;
    String username;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private static final String CHANNEL_ID = "media_playback_channel2";
    private static final int NOTIFICATION_ID = 2;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    ExoPlayer player;
    private EnvironmentalReverb reverb;
    int session_id;
    private SharedViewModel sharedViewModel;
    PlayerService playerService;
    Boolean isBound;
    ServiceConnection serviceConnection;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private long last_position;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        view = inflater.inflate(R.layout.fragment_home, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.up_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        sessionManager = new SessionManager(getContext());
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        //Setting up Homepage and getting given accesstoken
        set_up_buttons();
        TextView text1 = view.findViewById(R.id.your_radio);
        TextView text3 = view.findViewById(R.id.made_for_x);
        TextView text4 = view.findViewById(R.id.top_songs);
        text1.setText(username+" radio:");
        text3.setText(username+" mix");
        text4.setText(username+" Top 10");
        //Setting up Homepage and getting given accesstoken
        set_up_buttons();
        set_up_spotify_play();
        set_up_play_bar();
        if (SongQueue.getInstance().get_size() > 0) {
            set_up_skipping();
            last_position = OfflinePlayerManager.getInstance().current_player.getCurrentPosition();
            SongQueue.getInstance().setLast_postion(last_position);
        }
        return view;
    }
    //This function sets up media notification bar skip events
    public void set_up_skipping() {
        serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
                playerService = binder.getService();
                isBound = true;

                // Pass the ViewModel to the service
                sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
                playerService.setViewModel(sharedViewModel);
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
                    set_up_play_bar();
                }
            }
        });
    }
    //This function sets up the two image buttons on the homepage
    public void set_up_buttons() {
        ImageView button1 = view.findViewById(R.id.ic_radio);
        ImageView button2 = view.findViewById(R.id.ic_podium);
        ImageView button3 = view.findViewById(R.id.ic_for_you);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment newFragment = new Radio();
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, newFragment);
                fragmentTransaction.addToBackStack(null);  // Optional: adds the transaction to the back stack
                fragmentTransaction.commit();
            }
        });;
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment newFragment = new Top10Songs();
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, newFragment);
                fragmentTransaction.addToBackStack(null);  // Optional: adds the transaction to the back stack
                fragmentTransaction.commit();
            }
        });;
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment newFragment = new UserMix();
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, newFragment);
                fragmentTransaction.addToBackStack(null);  // Optional: adds the transaction to the back stack
                fragmentTransaction.commit();
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
    //This function opens a new song overlay
    public void open_new_overlay(MusicFile file, int position) {
        //Adding song to queue
        stopPlayerService();
        update_total_duration();
        SongQueue.getInstance().addSong(file);
        SongQueue.getInstance().setPosition(position);
        Fragment media_page = new MediaOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
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
    //This function handles Spotify overlay play/pause
    public void set_up_spotify_play() {
        if (OnlinePlayerManager.getInstance().mSpotifyAppRemote != null) {
            OnlinePlayerManager.getInstance().mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {
                    if (playerState.isPaused) {
                        ;
                    } else {
                        if (OfflinePlayerManager.getInstance().current_player != null) {
                            OfflinePlayerManager.getInstance().current_player.pause();
                        } else {
                            ;
                        }
                    }
                }
            });
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);;
    }
    private void stopPlayerService() {
        Intent intent = new Intent(requireContext(), PlayerService.class);
        requireContext().stopService(intent);
    }
}