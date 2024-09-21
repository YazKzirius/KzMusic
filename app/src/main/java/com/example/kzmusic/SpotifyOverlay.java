package com.example.kzmusic;

//Imports
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Response;


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
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final String API_KEY = "AIzaSyD8vgA5jBm6VC0b6UYVRZ8yYahMq1YrR5E"; // Replace with your YouTube Data API key
    private static final String TRACK_LIST_KEY = "track_list";
    View view;
    SpotifyAppRemote mSpotifyAppRemote;
    PlayerApi player;
    private TextView overlaySongTitle;
    private WebView youtubeWebView;
    private ImageView album_cover;
    SearchResponse.Track track;
    long current_position;
    long last_position;

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
        track = OnlinePlayerManager.getInstance().current_track;
        youtubeWebView = view.findViewById(R.id.youtube_webview);
        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        youtubeWebView.setWebViewClient(new WebViewClient());
        connect();
        overlaySongTitle.setText("Now playing: "+ track.getName() + " by " + track.getArtists().get(0).getName());
        //Adding song to database
        UsersTable table = new UsersTable(getContext());
        SessionManager sessionManager = new SessionManager(getContext());
        String email = sessionManager.getEmail();
        String title = track.getName() + " by " + track.getArtists().get(0).getName()+" (Official Music Video)";
        table.open();
        if (table.song_added(email, title) == true) {
            table.update_song_times_played(email, title);
        } else {
            table.add_new_song(email, title);
        }
        set_up_track_playing(track);
        current_position = System.currentTimeMillis();
        getVideoIdByName(track.getName()+" by "+track.getArtists().get(0).getName());
        return view;
    }

    //This function sets up music image view
    public void set_up_track_playing(SearchResponse.Track track) {
        // Load album image
        String album_url = track.getAlbum().getImages().get(0).getUrl();
        Glide.with(getContext()).asBitmap().load(album_url).circleCrop().into(album_cover);
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
                        OnlinePlayerManager.getInstance().setmSpotifyAppRemote(mSpotifyAppRemote);
                        Log.d("SpotifyAppRemote", "Connected");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("SpotifyAppRemote", throwable.getMessage(), throwable);
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
            OnlinePlayerManager.getInstance().setCurrent_track(track);
            overlaySongTitle.setText("Now playing similar songs to: " + track.getName() + " by " + track.getArtists().get(0).getName());
        }
    }

    //This function formats string is data and time format 0:00
    private String formatTime(long timeMs) {
        int totalSeconds = (int) (timeMs / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    //This function gets video id by song name
    public void getVideoIdByName(String songName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        YoutubeService service = retrofit.create(YoutubeService.class);
        Call<YoutubeResponse> call = service.searchVideos("snippet", songName, "video", API_KEY);

        call.enqueue(new Callback<YoutubeResponse>() {
            @Override
            public void onResponse(Call<YoutubeResponse> call, Response<YoutubeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    YoutubeResponse body = response.body();
                    if (body.items.length > 0 && body.items[0].id.videoId != null) {
                        String videoId = body.items[0].id.videoId;
                        String url = "https://www.youtube.com/watch?v=" + videoId;
                        youtubeWebView.loadUrl(url);


                        // Update UI or perform other actions with videoId
                    } else {
                        Toast.makeText(getContext(), "No video found", Toast.LENGTH_SHORT).show();
                        play_track(track.getUri());

                    }
                } else {
                    play_track(track.getUri());
                }
            }

            @Override
            public void onFailure(Call<YoutubeResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                play_track(track.getUri());
            }
        });
    }
    //This function updates the total duration field in SQL database
    public void update_total_duration() {
        long duration = System.currentTimeMillis() - current_position;
        String display_title =  track.getName() + " by " + track.getArtists().get(0).getName();
        //Applying audio effects
        //Updating song database
        SessionManager sessionManager = new SessionManager(getContext());
        String email = sessionManager.getEmail();
        UsersTable table = new UsersTable(getContext());
        table.open();
        table.update_song_duration(email, display_title, (int) duration/1000);
        table.close();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        update_total_duration();
    }

}