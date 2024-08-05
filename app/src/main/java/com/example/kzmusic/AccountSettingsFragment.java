package com.example.kzmusic;

//Imports
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AccountSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

//Account settings class
public class AccountSettingsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    String mParam1;
    String mParam2;
    View view;
    String username;
    String email;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton btnPlayPause;
    RelativeLayout playback_bar;

    public AccountSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AccountSettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountSettingsFragment newInstance(String param1, String param2) {
        AccountSettingsFragment fragment = new AccountSettingsFragment();
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
        view = inflater.inflate(R.layout.fragment_account_settings, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        btnPlayPause = view.findViewById(R.id.play_pause_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        //Setting up bottom playback navigator
        if (PlayerManager.getInstance().spotify_player != null && PlayerManager.getInstance().current_player != null) {
            set_up_spotify_play();
            set_up_play_bar();
        } else {
            set_up_play_bar();
        }
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ;
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
}