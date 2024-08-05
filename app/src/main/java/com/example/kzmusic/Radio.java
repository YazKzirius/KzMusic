package com.example.kzmusic;

//imports
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Radio#newInstance} factory method to
 * create an instance of this fragment.
 */
//This class implements the User radio page
public class Radio extends Fragment {

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
    RecyclerView recyclerView;
    MusicAdapter musicAdapter;
    SpotifyAppRemote mSpotifyAppRemote;
    PlayerApi player;
    String accesstoken;
    View view;
    Boolean has_premium;
    SessionManager sessionManager;
    String email;
    String username;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton btnPlayPause;
    RelativeLayout playback_bar;

    public Radio(String token) {
        // Required empty public constructor
        this.accesstoken = token;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Radio.
     */
    // TODO: Rename and change types and number of parameters
    public static Radio newInstance(String param1, String param2) {
        Radio fragment = new Radio(param1);
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
        view =  inflater.inflate(R.layout.fragment_radio, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        btnPlayPause = view.findViewById(R.id.play_pause_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        recyclerView=view.findViewById(R.id.recycler_view1);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter=new MusicAdapter(trackList,getContext(),new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchResponse.Track track){
                Toast.makeText(getContext(),"Playing Songs Similar to: "+track.getName(),Toast.LENGTH_SHORT).show();
                //Pausing current player, so no playback overlap
                if (PlayerManager.getInstance().get_size() > 0) {
                    PlayerManager.getInstance().current_player.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    play_track(track.getUri());
                } else {
                    play_track(track.getUri());
                }
                if (PlayerManager.getInstance().spotify_playing != null) {
                    set_up_spotify_play();
                }
            }
        });
        recyclerView.setAdapter(musicAdapter);
        display_random_music(accesstoken);
        //Setting up bottom playback navigator
        set_up_play_bar();
        return view;
    }
    //These functions authenticate Spotify remote use
    @Override
    public void onStart() {
        super.onStart();
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(getContext(), connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("SpotifyAppRemote", "Connected");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("SpotifyAppRemote", throwable.getMessage(), throwable);
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        ;
    }
    //This function searches for random music using API queries and updates the current tracklist
    public void display_random_music(String token) {
        accesstoken = token;
        String[] randomQueries = {"happy", "sad", "party", "chill", "love", "workout"};
        String randomQuery = randomQueries[(int) (Math.random() * randomQueries.length)];
        SpotifyApiService apiService = RetrofitClient.getClient(accesstoken).create(SpotifyApiService.class);
        Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    musicAdapter.updateTracks(response.body().getTracks().getItems());
                } else {
                    ;
                }
            }
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(getContext(), "API call failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    //These functions handle song playback
    private void play_track(String uri) {
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
            //Adding player to manager
            PlayerManager.getInstance().addSpotifyPlayer(player);
            PlayerManager.getInstance().setSpotify_player(player);
            PlayerManager.getInstance().setCurrent_remote(mSpotifyAppRemote);
        }
    }
    //This function handles Spotify overlay play/pause
    public void set_up_spotify_play() {
        player.subscribeToPlayerState()
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
                            if (player != null) {
                                player.pause();
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
        if (player != null) {
            player.pause();
        }
        SongQueue.getInstance().addSong(file);
        SongQueue.getInstance().setPosition(position);
        Fragment media_page = new MediaOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
    }
}