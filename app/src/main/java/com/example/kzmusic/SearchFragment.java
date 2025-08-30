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

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import android.view.inputmethod.InputMethodManager;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

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
                if (playerService != null) {
                    playerService.pause();
                    playerService.updatePlaybackState();
                    playerService.updateNotification(SongQueue.getInstance().current_song);
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                }
            }
        });
        recyclerView.setAdapter(musicAdapter);
        display_random();

        TextView resultsView = view.findViewById(R.id.results);
        EditText searchInput = view.findViewById(R.id.search_input);
        Button searchButton = view.findViewById(R.id.search_button);

        resultsView.setText("Search results:");

// Edit text live functionality with DEBOUNCING
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();

                // Cancel any pending search
                searchHandler.removeCallbacks(searchRunnable);

                // If the text is empty, don't search. You could display random tracks again here if you want.
                if (query.isEmpty()) {
                    return;
                }

                // Define a new search task to run after a delay
                searchRunnable = () -> search_track(query);

                // Schedule the search task to run after 300ms
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });


// Search button functionality (immediate search, no delay needed)
        searchButton.setOnClickListener(v -> {
            String input = searchInput.getText().toString();
            if (input.isEmpty()) {
                display_random();
            } else {
                search_track(input);
            }
            resultsView.setText("Search results:");
        });


// "Enter" key functionality on keyboard
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                // Hide the keyboard
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                // Perform the search
                String input = searchInput.getText().toString();
                if (input.isEmpty()) {
                    display_random();
                } else {
                    search_track(input);
                }
                resultsView.setText("Search results:");
                return true; // Event was handled
            }
            return false; // Event was not handled
        });
        //Setting up bottom playback navigator
        set_up_spotify_play();
        set_up_play_bar();
        if (SongQueue.getInstance().get_size() > 0 && SongQueue.getInstance().current_song != null) {
            set_up_skipping();
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
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    //This function makes an API call using previous access token to search for random music
    //It does this based on the track_name entered
    private void search_track(String track_name) {
        TextView text1 = view.findViewById(R.id.results);

        // Ask the manager for a valid token
        SpotifyAuthManager.getInstance().getValidAccessToken(new TokenCallback() {
            @Override
            public void onTokenReceived(String accessToken) {
                // SUCCESS: We have a valid token, now we can make the API call
                SpotifyApiService apiService = RetrofitClient.getClient(accessToken).create(SpotifyApiService.class);
                Call<SearchResponse> call = apiService.searchTracks(track_name, "track");

                call.enqueue(new Callback<SearchResponse>() {
                    @Override
                    public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            trackList.clear();
                            trackList.addAll(response.body().getTracks().getItems());
                            musicAdapter.notifyDataSetChanged();
                        } else {
                            // This handles other errors like 404, 500, etc.
                            Log.e("SearchTrack", "API call failed with code: " + response.code());
                            text1.setText("Search request failed.");
                        }
                    }

                    @Override
                    public void onFailure(Call<SearchResponse> call, Throwable t) {
                        // This handles network failures
                        Log.e("SearchTrack", "API call failed on network.", t);
                        text1.setText("No internet connection, please try again.");
                    }
                });
            }

            @Override
            public void onError() {
                // FAILURE: Could not get a valid token. The refresh token is likely invalid.
                // The user must re-authenticate.
                Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                SpotifyAuthManager.getInstance().logout(getContext());
            }
        });
    }
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(getContext(), target);
        startActivity(intent);
    }
    //This function gets random music based on catergory
    //It does this before the user chooses to search for a random track
    private void display_random() {
        TextView text1 = view.findViewById(R.id.results);

        // Array of creative search queries
        String[] randomQueries = {
                "vibe: chill synthwave", "inspired by: tame impala",
                "soundtrack for: late-night coding", "nostalgia: 90s hip-hop",
                "playlist: feel-good throwbacks", "theme: fantasy RPG battle",
                "mood: euphoric dance", "genre: alt-R&B with soul"
        };
        String randomQuery = randomQueries[(int) (Math.random() * randomQueries.length)];

        // Ask the manager for a valid token
        SpotifyAuthManager.getInstance().getValidAccessToken(new TokenCallback() {
            @Override
            public void onTokenReceived(String accessToken) {
                // SUCCESS: We have a valid token, now make the API call
                SpotifyApiService apiService = RetrofitClient.getClient(accessToken).create(SpotifyApiService.class);
                Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");

                call.enqueue(new Callback<SearchResponse>() {
                    @Override
                    public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            trackList.clear();
                            trackList.addAll(response.body().getTracks().getItems());
                            musicAdapter.notifyDataSetChanged();
                        } else {
                            // Handle other API errors (e.g., 404, 500)
                            Log.e("DisplayRandom", "API call failed with code: " + response.code());
                            text1.setText("Could not fetch recommendations.");
                        }
                    }

                    @Override
                    public void onFailure(Call<SearchResponse> call, Throwable t) {
                        // Handle network failures
                        Log.e("DisplayRandom", "API call failed on network.", t);
                        text1.setText("No internet connection, please try again.");
                    }
                });
            }

            @Override
            public void onError() {
                // FAILURE: Could not get a valid token. User must log in again.
                Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                SpotifyAuthManager.getInstance().logout(getContext());
            }
        });
    }
    //This function assigns data from playback overlay to bottom navigation
    public void set_up_play_bar() {
        if (SongQueue.getInstance().songs_played.size() == 0 || SongQueue.getInstance().current_song == null) {
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
        if (song != null) {
            String display_title = song.getName();
            String artist = song.getArtist().replaceAll("/", ", ");
            display_title = display_title.replaceAll("by "+artist, "").replaceAll(
                    "- "+artist, "");
            if (isOnlyDigits(display_title)) {
                display_title = display_title +" by "+ artist;
            } else {
                display_title = format_title(display_title) +" by "+ artist;
            }
            Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
            Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(song.getAlbumId()));
            Glide.with(getContext()).asBitmap().load(album_uri).circleCrop().into(art);
            title.setText("Now playing "+display_title);
            Artist.setText(song.getArtist().replaceAll("/", ", "));
        } else {
            ;
        }
    }

    // This function formats song title, removing unnecessary data and watermarks
    public String format_title(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "";
        }

        String originalTitleBeforeNumericCheck = title; // Store original for potential revert
        String workingTitle = title;

        // 1. More aggressive generic watermark removal
        workingTitle = workingTitle.replaceAll("\\[[^\\]]*\\]", "");
        workingTitle = workingTitle.replaceAll("\\((?i)(official|lyric|video|audio|hd|hq|explicit|remastered|live|original|mix|feat|ft\\.)[^)]*\\)", "");

        // 2. Remove common file extensions
        workingTitle = workingTitle.replaceAll("(?i)\\.(mp3|flac|wav|m4a|ogg|aac|wma)$", "");

        // --- Store the state of workingTitle before attempting to strip leading numbers ---
        // This is what we might revert to if stripping numbers leaves only other numbers.
        String titleAfterWatermarksAndExtensions = workingTitle.trim();
        workingTitle = titleAfterWatermarksAndExtensions;

        // 4. Replace underscores and multiple spaces with a single space
        workingTitle = workingTitle.replaceAll("[_]+", " ");
        workingTitle = workingTitle.replaceAll("\\s+", " ");

        // 5. Trim leading/trailing whitespace
        workingTitle = workingTitle.trim();


        // 6. FINAL CHECKS
        // If the result of all cleaning is ONLY digits, and the original wasn't just those digits (meaning watermarks/extensions were removed)
        // then it's likely the "title" part was just numbers. In this case, we prefer the version before number stripping,
        // or even the original if the number-only version is too bare.
        if (isOnlyDigits(workingTitle) && !workingTitle.isEmpty()) {
            // If 'titleAfterLeadingNumberRemoval' (which is 'workingTitle' before this final digit check)
            // is also all digits and is the same as the current 'workingTitle',
            // it means the leading number removal didn't change anything, or it removed a prefix and left digits.
            // Consider 'titleAfterWatermarksAndExtensions'. If it wasn't just digits, prefer that.
            if (!isOnlyDigits(titleAfterWatermarksAndExtensions) && !titleAfterWatermarksAndExtensions.isEmpty()) {
                // If the version before number stripping had letters, it was better.
                return titleAfterWatermarksAndExtensions;
            } else {
                // If even after watermarks/extensions it was just numbers, or became just numbers,
                // and the current workingTitle is also just numbers,
                // it implies the original might have been "numbers.mp3" or "[watermark] numbers".
                // In this specific case, returning the 'workingTitle' (which is just numbers) is acceptable
                // as per the request "if a song name is just numbers after ... handling, return the string [of numbers]".
                // However, if the original title had more context, we might prefer 'originalTitleBeforeNumericCheck'.
                // This logic becomes a bit about preference. For now, let's stick to returning the numbers if that's what's left.
            }
        }


        // If after all operations, the title becomes empty (e.g., it was just "[SPOTIFY-DOWNLOADER.COM].mp3")
        // return the original title (or a placeholder) if the original had meaningful content.
        if (workingTitle.isEmpty() && !originalTitleBeforeNumericCheck.trim().isEmpty()) {
            // Check if the original, after basic cleaning (watermarks/extensions) was also empty or just noise
            String tempCleanedOriginal = originalTitleBeforeNumericCheck.replaceAll("\\[[^\\]]*\\]", "")
                    .replaceAll("\\((?i)(official|lyric|video|audio|hd|hq|explicit|remastered|live|original|mix|feat|ft\\.)[^)]*\\)", "")
                    .replaceAll("(?i)\\.(mp3|flac|wav|m4a|ogg|aac|wma)$", "").trim();
            if(tempCleanedOriginal.isEmpty() || tempCleanedOriginal.matches("^[\\s.-]*$") || isOnlyDigits(tempCleanedOriginal)) {
                // If the original itself was essentially just a watermark/number/extension, returning an empty string might be fine
                // or return the numeric part if that's all that's left of the original.
                if(isOnlyDigits(tempCleanedOriginal) && !tempCleanedOriginal.isEmpty()) return tempCleanedOriginal;
                // else if original was just noise, and working title is empty, original request for this state is not to return original title
            } else {
                return originalTitleBeforeNumericCheck; // Prefer fuller original if cleaning nuked a valid title
            }
        }
        Pattern leadingNoisePattern = Pattern.compile("^[^a-zA-Z0-9]*\\d{1,4}[\\s.-]*");
        Matcher noiseMatcher = leadingNoisePattern.matcher(workingTitle);
        if (noiseMatcher.find()) {
            String afterNoise = workingTitle.substring(noiseMatcher.end());
            // Only remove if what follows isn't empty or just noise itself
            if (!afterNoise.trim().isEmpty() && afterNoise.matches(".*[a-zA-Z].*")) { // Check if there's at least one letter after
                workingTitle = afterNoise;
            }
        }

        // 4. Replace underscores and multiple spaces with a single space
        workingTitle = workingTitle.replaceAll("[_]+", " ");
        workingTitle = workingTitle.replaceAll("\\s+", " ");

        // 5. Trim leading/trailing whitespace that might have been introduced or was already there.
        workingTitle = workingTitle.trim();

        // 6. Handle cases where the title might become just a separator after cleaning
        if (workingTitle.matches("^[\\s.-]*$")) { // If only spaces, dots, hyphens remain
            return title; // Revert to original title if cleaning results in effectively nothing meaningful
        }


        // If after all operations, the title becomes empty (e.g., it was just "[SPOTIFY-DOWNLOADER.COM].mp3")
        // return the original title or a placeholder, rather than an empty string if the original had content.
        if (workingTitle.isEmpty() && !title.trim().isEmpty()) {
            return title; // Or a specific placeholder like "Unknown Title"
        }
        return workingTitle;
    }

    // Helper function to check if a string contains only digits
    private boolean isOnlyDigits(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        String trimmedStr = str.trim(); // Trim spaces before checking
        if (trimmedStr.isEmpty()) {
            return false;
        }
        for (char c : trimmedStr.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
    //This function opens a new song overlay
    public void open_new_overlay(MusicFile file, int position) {
        if (file == null) {
            ;
        } else {
            //Adding song to queue
            SongQueue.getInstance().addSong(file);
            SongQueue.getInstance().setPosition(position);
            Fragment media_page = new MediaOverlay();
            if (playerService != null) {
                playerService.updatePlaybackState();
                playerService.updateNotification(file);
                playerService.handlePlay();
            }
            FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, media_page);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
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
                        if (playerService != null) {
                            playerService.pause();
                            playerService.updatePlaybackState();
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