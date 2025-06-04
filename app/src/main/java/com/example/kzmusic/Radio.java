package com.example.kzmusic;

//imports
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.google.android.gms.tasks.OnSuccessListener;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
    String accesstoken;
    View view;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private SharedViewModel sharedViewModel;
    PlayerService playerService;
    Boolean isBound;
    ServiceConnection serviceConnection;
    SessionManager sessionManager;
    Boolean liked_on = false;
    Boolean shuffle_on = false;
    private long last_position;
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    int REQUEST_CODE = 1337;

    public Radio() {
        // Required empty public constructor
        ;
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
        Radio fragment = new Radio();
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
        ic_down = view.findViewById(R.id.up_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        recyclerView=view.findViewById(R.id.recycler_view1);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //Checking if access token is not null and setting up spotify for that
        musicAdapter=new MusicAdapter(trackList,getContext(),new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchResponse.Track track){
                //Pausing current player, so no playback overlap
                if (OfflinePlayerManager.getInstance().get_size() > 0) {
                    OfflinePlayerManager.getInstance().current_player.pause();
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                }
            }
        });
        sessionManager = new SessionManager(getContext());
        recyclerView.setAdapter(musicAdapter);
        if (sessionManager.getSavedTracklist("TRACK_LIST_RADIO").size() == 0) {
            display_random_music();
        } else {
            musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_RADIO"));
        }
        //Setting up bottom playback navigator
        set_up_spotify_play();
        set_up_play_bar();
        set_up_refresh();
        if (SongQueue.getInstance().get_size() > 0) {
            set_up_skipping();
            last_position = OfflinePlayerManager.getInstance().current_player.getCurrentPosition();
            SongQueue.getInstance().setLast_postion(last_position);
        }
        set_up_playback_buttons();
        return view;
    }
    //This function sets up playback buttons at top
    public void set_up_playback_buttons() {
        //Session class
        sessionManager = new SessionManager(getContext());
        //Setting up liked all button
        ImageButton btn1 = view.findViewById(R.id.like_all);
        btn1.setImageResource(R.drawable.ic_liked_off);
        all_liked(isLiked -> {
            if (isLiked) {
                btn1.setImageResource(R.drawable.ic_liked);
            } else {
                btn1.setImageResource(R.drawable.ic_liked_off); // You may want to add an "unliked" icon
            }
        });
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                liked_on = !liked_on;
                //If liked all button on, like all songs in recycler view and display liked icon
                SavedSongsFirestore table = new SavedSongsFirestore(getContext());
                String email = sessionManager.getEmail();
                if (liked_on == true) {
                    btn1.setImageResource(R.drawable.ic_liked);
                    for (SearchResponse.Track track : sessionManager.getSavedTracklist("TRACK_LIST_RADIO")) {
                        String title = track.getName()+" by "+track.getArtists().get(0).getName();
                        String url = track.getAlbum().getImages().get(0).getUrl();
                        table.save_new_song(email, title, url);
                        musicAdapter.clear_tracks();
                        musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_RADIO"));

                    }
                //Otherwise, unlike all songs and display unliked icon
                } else {
                    btn1.setImageResource(R.drawable.ic_liked_off);
                    for (SearchResponse.Track track : sessionManager.getSavedTracklist("TRACK_LIST_RADIO")) {
                        String title = track.getName()+" by "+track.getArtists().get(0).getName();
                        String url = track.getAlbum().getImages().get(0).getUrl();
                        table.remove_saved_song(email, title, url);
                        musicAdapter.clear_tracks();
                        musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_RADIO"));

                    }
                }
            }
        });
        //Play button functionality
        ImageButton btn2 = view.findViewById(R.id.play_all);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shuffle_on == false) {
                    SearchResponse.Track track = sessionManager.getSavedTracklist("TRACK_LIST_RADIO").get(0);
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    Random rand = new Random();
                    int index = rand.nextInt(sessionManager.getSavedTracklist("TRACK_LIST_RADIO").size());
                    SearchResponse.Track track = sessionManager.getSavedTracklist("TRACK_LIST_RADIO").get(index);
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                }
            }
        });
        //Shuffle button functionlity
        ImageButton btn3 = view.findViewById(R.id.shuffle);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shuffle_on = !shuffle_on;
                if (shuffle_on == true) {
                    btn3.setImageResource(R.drawable.ic_shuffle_on);
                } else {
                    btn3.setImageResource(R.drawable.ic_shuffle);
                }
            }
        });
    }
    //This function checks if all songs in view are liked
    public void all_liked(OnSuccessListener<Boolean> callback) {
        SavedSongsFirestore table = new SavedSongsFirestore(getContext());
        String email = sessionManager.getEmail();
        List<SearchResponse.Track> trackList = sessionManager.getSavedTracklist("TRACK_LIST_RADIO");

        if (trackList.isEmpty()) {
            callback.onSuccess(false);
            return;
        }

        AtomicInteger count = new AtomicInteger(0);
        for (SearchResponse.Track track : trackList) {
            String title = track.getName() + " by " + track.getArtists().get(0).getName();
            table.is_saved(email, title, isLiked -> {
                if (!isLiked) {
                    callback.onSuccess(false);
                } else {
                    if (count.incrementAndGet() == trackList.size()) {
                        callback.onSuccess(true); // ✅ All songs are liked
                    }
                }
            });
        }
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
    //This function sets up refresh button
    public void set_up_refresh() {
        Button refresh_btn = view.findViewById(R.id.refresh_btn);
        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicAdapter.clear_tracks();
                display_random_music();
            }
        });
    }
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(getContext(), target);
        startActivity(intent);
    }
    //This function searches for random music using API queries and updates the current tracklist
    public void display_random_music() {
        accesstoken = OnlinePlayerManager.getInstance().getAccess_token();
        if (accesstoken == null) {
            TextView text1 = view.findViewById(R.id.made_for_user);
            text1.setText("No internet connection, please try again.");
        } else {
            String[] randomQueries = {"happy", "sad", "party", "chill", "love", "workout"};
            String randomQuery = randomQueries[(int) (Math.random() * randomQueries.length)];
            SpotifyApiService apiService = RetrofitClient.getClient(accesstoken).create(SpotifyApiService.class);
            Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");
            call.enqueue(new Callback<SearchResponse>() {
                @Override
                public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        musicAdapter.updateTracks(response.body().getTracks().getItems());
                        sessionManager.save_Tracklist_radio(response.body().getTracks().getItems());
                    } else if (response.code() == 401) { // Handle expired access token
                        TokenManager.getInstance().refreshAccessToken(OnlinePlayerManager.getInstance().getRefresh_token());
                    } else {
                        ;
                    }
                }
                @Override
                public void onFailure(Call<SearchResponse> call, Throwable t) {
                    TextView text1 = view.findViewById(R.id.made_for_user);
                    text1.setText("No internet connection, please try again.");
                }
            });
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
    //This function opens Spotify player overlay
    public void open_spotify_overlay() {
        Fragment spotify_overlay = new SpotifyOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, spotify_overlay);
        fragmentTransaction.commit();
    }
    //This function opens a new song overlay
    public void open_new_overlay(MusicFile file, int position) {
        //Adding song to queue
        stopPlayerService();
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
    private void stopPlayerService() {
        Intent intent = new Intent(requireContext(), PlayerService.class);
        requireContext().stopService(intent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}