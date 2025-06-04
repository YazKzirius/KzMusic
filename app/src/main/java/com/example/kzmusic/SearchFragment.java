package com.example.kzmusic;

//Imports
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.List;

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
    PlayerService playerService;
    Boolean isBound;
    ServiceConnection serviceConnection;
    private long last_position;

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
        ic_down = view.findViewById(R.id.up_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view3);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicAdapter(trackList, getContext(), new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchResponse.Track track) {
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
        recyclerView.setAdapter(musicAdapter);
        TextView View = view.findViewById(R.id.results);
        EditText search = view.findViewById(R.id.search_input);
        //Edit text live functionality
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Call your search_track function here
                String trackName = s.toString();
                if (s.length() == 0) {
                    ;
                } else {
                    ;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text changes
            }
        });
        View.setText("Search results:");
        Button search_button = view.findViewById(R.id.search_button);
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Searching for random tracks based on the name input
                String input = search.getText().toString();
                if (input.equals("")) {
                    View.setText("Search results:");
                } else {
                    //Displaying results
                    View.setText("Search results:");
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
                        ;
                    } else {
                        ;
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
        if (SongQueue.getInstance().get_size() > 0) {
            set_up_skipping();
            last_position = OfflinePlayerManager.getInstance().current_player.getCurrentPosition();
            SongQueue.getInstance().setLast_postion(last_position);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ;
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
    private void search_track(String track_name) {
        accesstoken = OnlinePlayerManager.getInstance().getAccess_token();
        if (accesstoken == null) {
            TextView text1 = view.findViewById(R.id.results);
            text1.setText("No internet connection, please try again.");
        } else {
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
                    } else if (response.code() == 401) { // Handle expired access token
                        OnlinePlayerManager.getInstance().setAccess_token(null);
                        OnlinePlayerManager.getInstance().setRefresh_token(null);
                        navigate_to_activity(GetStarted.class);
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
    }
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(getContext(), target);
        startActivity(intent);
    }
    //This function gets random music based on catergory
    //It does this before the user chooses to search for a random track
    private void display_random() {
        accesstoken = OnlinePlayerManager.getInstance().getAccess_token();
        if (accesstoken == null) {
            TextView text1 = view.findViewById(R.id.results);
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
                        trackList.clear();
                        trackList.addAll(response.body().getTracks().getItems());
                        musicAdapter.notifyDataSetChanged();
                    } else if (response.code() == 401) { // Handle expired access token
                        TokenManager.getInstance().refreshAccessToken(OnlinePlayerManager.getInstance().getRefresh_token());
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
}