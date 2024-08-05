package com.example.kzmusic;

//Imports
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.bumptech.glide.Glide;
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
    String accesstoken;
    View view;
    SessionManager sessionManager;
    String email;
    String username;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton btnPlayPause;
    RelativeLayout playback_bar;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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
        btnPlayPause = view.findViewById(R.id.play_pause_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        sessionManager = new SessionManager(getContext());
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        //Setting up Homepage and getting given accesstoken
        set_up_buttons();
        TextView text1 = view.findViewById(R.id.made_for_x);
        TextView text2 = view.findViewById(R.id.user_music);
        text1.setText(username+" radio:");
        text2.setText(username+" music:");
        if (getArguments() != null) {
            accesstoken = getArguments().getString("Token");
        }
        //Setting up bottom playback navigator
        if (PlayerManager.getInstance().spotify_player != null && PlayerManager.getInstance().current_player != null) {
            set_up_spotify_play();
            set_up_play_bar();
        } else {
            set_up_play_bar();
        }
        return view;
    }
    //This function sets up the two image buttons on the homepage
    public void set_up_buttons() {
        ImageView button1 = view.findViewById(R.id.ic_radio);
        ImageView button2 = view.findViewById(R.id.ic_library);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment newFragment = new Radio(accesstoken);
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, newFragment);
                fragmentTransaction.addToBackStack(null);  // Optional: adds the transaction to the back stack
                fragmentTransaction.commit();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment newFragment = new UserMusic();
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, newFragment);
                fragmentTransaction.addToBackStack(null);  // Optional: adds the transaction to the back stack
                fragmentTransaction.commit();
            }
        });
    }
    //This function handles Spotify overlay play/pause
    public void set_up_spotify_play() {
        PlayerManager.getInstance().spotify_player.subscribeToPlayerState()
                .setEventCallback(new Subscription.EventCallback<PlayerState>() {
                    @Override
                    public void onEvent(PlayerState playerState) {
                        if (playerState.isPaused) {
                            ;
                        } else {
                            PlayerManager.getInstance().current_player.pause();
                            btnPlayPause.setImageResource(R.drawable.ic_play);
                        }
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
            Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
            Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(song.getAlbumId()));
            Glide.with(getContext()).asBitmap().load(album_uri).circleCrop().into(art);
            title.setText(song.getName().replace("[SPOTIFY-DOWNLOADER.COM] ", "").replace(".mp3", ""));
            Artist.setText(song.getArtist());
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
            //Implementing pause button functionality
            if (PlayerManager.getInstance().get_size() > 0) {
                if (PlayerManager.getInstance().current_player.isPlaying()) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
            btnPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Checking if they're is already a song currently playing
                    if (PlayerManager.getInstance().get_size() > 0) {
                        if (PlayerManager.getInstance().current_player.isPlaying()) {
                            PlayerManager.getInstance().current_player.pause();
                            btnPlayPause.setImageResource(R.drawable.ic_play);
                        } else {
                            if (PlayerManager.getInstance().spotify_player != null) {
                                PlayerManager.getInstance().spotify_player.pause();
                            }
                            PlayerManager.getInstance().current_player.play();
                            btnPlayPause.setImageResource(R.drawable.ic_pause);
                        }
                    } else {
                        ;
                    }


                }
            });
        }
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
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);;
    }
}