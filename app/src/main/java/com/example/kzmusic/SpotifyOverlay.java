package com.example.kzmusic;

//Imports
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.protocol.types.Repeat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SpotifyOverlay#newInstance} factory method to
 * create an instance of this fragment.
 */
//This class handles Spotify track playback
public class SpotifyOverlay extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    List<SearchResponse.Track> trackList = new ArrayList<>();
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String TRACK_LIST_KEY = "track_list";
    View view;
    SpotifyAppRemote mSpotifyAppRemote;
    PlayerApi player;
    private TextView overlaySongTitle;
    private ImageButton btnPlayPause;
    private ImageButton btnLoop;
    private ImageButton btnSkip_left;
    private ImageButton btnSkip_right;
    private ImageButton btnShuffle;
    private ImageView album_cover;
    private ImageView song_gif;
    SearchResponse.Track track;
    Boolean is_paused = false;
    Boolean is_looping = false;
    Boolean shuffle_on = false;

    public SpotifyOverlay() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SpotifyOverlay.
     */
    // TODO: Rename and change types and number of parameters
    public static SpotifyOverlay newInstance(String param1, String param2) {
        SpotifyOverlay fragment = new SpotifyOverlay();
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
        view = inflater.inflate(R.layout.fragment_spotify_overlay, container, false);
        overlaySongTitle = view.findViewById(R.id.songTitle);
        album_cover = view.findViewById(R.id.musicImage);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnLoop = view.findViewById(R.id.btnLoop);
        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnSkip_left = view.findViewById(R.id.btnSkipLeft);
        btnSkip_right = view.findViewById(R.id.btnSkipRight);
        song_gif = view.findViewById(R.id.Spotify_playing);
        track = SpotifyPlayerLife.getInstance().current_track;
        connect();
        set_up_track_playing(track);
        return view;
    }
    //This function sets up music image view
    public void set_up_track_playing(SearchResponse.Track track) {
        // Load album image
        String album_url = track.getAlbum().getImages().get(0).getUrl();
        Glide.with(getContext()).asBitmap().load(album_url).circleCrop().into(album_cover);
        Glide.with(getContext()).asGif().load(R.drawable.spotify_playing).into(song_gif);
    }
    //These functions connects to Spotify remote using it's API
    public void connect() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(getContext(), connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        play_track(track.getUri());
                        SpotifyPlayerLife.getInstance().setmSpotifyAppRemote(mSpotifyAppRemote);
                        set_up_media_buttons();
                        Log.d("SpotifyAppRemote", "Connected");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("SpotifyAppRemote", throwable.getMessage(), throwable);
                    }
                });
    }
    //This function sets up media buttons in overlay
    public void set_up_media_buttons() {
        //Pause/play functionality
        player.subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                if (playerState.isPaused) {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    Glide.with(getContext()).clear(song_gif);
                    is_paused = true;
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    set_up_track_playing(track);
                    is_paused = false;
                }
            }
        });
        btnPlayPause.setOnClickListener(v -> {
            if (is_paused == true) {
                player.resume();
                set_up_track_playing(track);
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                player.pause();
                Glide.with(getContext()).clear(song_gif);
                btnPlayPause.setImageResource(R.drawable.ic_play);
            }
        });
        //Loop functionality
        //If loop was on previously, keep loop on otherwise, continue
        if (is_looping == true) {
            //Setting repeat mode on and replacing icon
            player.setRepeat(Repeat.ONE);
            btnLoop.setImageResource(R.drawable.ic_loop_on);
        } else {
            //Setting repeat mode off and replacing icon
            player.setRepeat(Repeat.OFF);
            btnLoop.setImageResource(R.drawable.ic_loop);
        }
        //Loop button click functionality
        btnLoop.setOnClickListener(v -> {
            is_looping = !is_looping;
            if (is_looping == true) {
                player.setRepeat(Repeat.ONE);
                btnLoop.setImageResource(R.drawable.ic_loop_on);
            } else {
                player.setRepeat(Repeat.OFF);
                btnLoop.setImageResource(R.drawable.ic_loop);
            }
        });
        //Skip button functionality
        btnSkip_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                //Moving to next song in recycler view if shuffle is off
                player.skipPrevious();
            }
        });
        btnSkip_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                //Moving to next song in recycler view if shuffle is off
                player.skipNext();
            }
        });
        //Implementing shuffle button functionality
        if (shuffle_on == true) {
            player.setShuffle(true);
            btnShuffle.setImageResource(R.drawable.ic_shuffle_on);
        } else {
            //Setting repeat mode off and replacing icon
            player.setShuffle(false);
            btnShuffle.setImageResource(R.drawable.ic_shuffle);
        }
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shuffle_on = !shuffle_on;
                if (shuffle_on == true) {
                    //Setting repeat mode on and replacing icon
                    player.setShuffle(true);
                    btnShuffle.setImageResource(R.drawable.ic_shuffle_on);
                } else {
                    //Setting repeat mode off and replacing icon
                    player.setShuffle(false);
                    btnShuffle.setImageResource(R.drawable.ic_shuffle);
                }
            }
        });
    }

    //This function handle song playback
    public void play_track(String uri) {
        if (mSpotifyAppRemote != null) {
            player = mSpotifyAppRemote.getPlayerApi();
            player.play(uri);
            player.subscribeToPlayerState()
                    .setEventCallback(new Subscription.EventCallback<PlayerState>() {
                        @Override
                        public void onEvent(PlayerState playerState) {
                            final Track track = playerState.track;
                        }
                    });
            //Adding track to manager
            //Adding data to display
            SpotifyPlayerLife.getInstance().setCurrent_track(track);
            overlaySongTitle.setText("Now playing similar songs to: "+track.getName()+" by "+track.getArtists().get(0).getName());
        }
    }
}