package com.example.kzmusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LikedSongs#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LikedSongs extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private RecyclerView recyclerView1;
    private MusicAdapter musicAdapter1;
    private RecyclerView recyclerView2;
    private MusicAdapter musicAdapter2;
    View view;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    String email;
    String username;
    String token;
    SessionManager sessionManager;
    private List<SearchResponse.Track> tracklist = new ArrayList<>();
    private List<SearchResponse.Track> tracklist_odd = new ArrayList<>();
    private List<SearchResponse.Track> tracklist_even = new ArrayList<>();
    private SharedViewModel sharedViewModel;
    PlayerService playerService;
    Boolean isBound;
    ServiceConnection serviceConnection;
    Boolean liked_on = false;
    Boolean shuffle_on = false;
    private long last_position;

    public LikedSongs() {
        // Required empty public constructor
        ;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LikedSongs.
     */
    // TODO: Rename and change types and number of parameters
    public static LikedSongs newInstance(String param1, String param2) {
        LikedSongs fragment = new LikedSongs();
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
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_liked_songs, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.up_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        //Getting user info
        sessionManager = new SessionManager(getContext());
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        TextView text = view.findViewById(R.id.x_liked);
        text.setText(username+" Liked songs");
        //First recycler view
        recyclerView1 = view.findViewById(R.id.recycler_view1);
        recyclerView1.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter1 = new MusicAdapter(tracklist, getContext(), new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchResponse.Track track) {
                //Pausing current player, so no playback overlap
                if (OfflinePlayerManager.getInstance().get_size() > 0) {
                    OfflinePlayerManager.getInstance().current_player.pause();
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                    ;
                } else {
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                    ;
                }
            }
        });
        recyclerView1.setAdapter(musicAdapter1);
        //Setting up bottom playback navigator
        set_up_spotify_play();
        set_up_play_bar();
        if (SongQueue.getInstance().get_size() > 0) {
            set_up_skipping();
            last_position = OfflinePlayerManager.getInstance().current_player.getCurrentPosition();
            SongQueue.getInstance().setLast_postion(last_position);
        }
        //Getting user liked songs
        //Checking number of saved songs
        SavedSongsFirestore table = new SavedSongsFirestore(getContext());
        sessionManager = new SessionManager(getContext());
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        //Saving songs to Saved collection if not already saved
        table.db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                String user_id = querySnapshot.getDocuments().get(0).getId();
                                // 🔥 Check if the same song exists for the user
                                table.db.collection("SavedSongs")
                                        .whereEqualTo("USER_ID", user_id) // Ensure user does not have this song already
                                        .get()
                                        .addOnSuccessListener(songSnapshot -> {
                                            if (songSnapshot.isEmpty()) {
                                                ;
                                            } else {
                                                if (sessionManager.getSavedTracklist("TRACK_LIST_LIKED").size() == 0 || songSnapshot.getDocuments().size() !=
                                                        sessionManager.getSavedTracklist("TRACK_LIST_LIKED").size()) {
                                                    get_liked_songs();
                                                } else {
                                                    musicAdapter1.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_LIKED"));
                                                    sessionManager.save_Tracklist_liked(sessionManager.getSavedTracklist("TRACK_LIST_LIKED"));
                                                }
                                            }
                                        });
                            } else {
                                ;
                            }
                        });
        set_up_playback_buttons();
        return view;
    }
    //This function sets up playback buttons at top
    public void set_up_playback_buttons() {
        //Session class
        sessionManager = new SessionManager(getContext());
        //Setting up liked all button
        ImageButton btn1 = view.findViewById(R.id.clear_all);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clearing liked songs
                SavedSongsFirestore table = new SavedSongsFirestore(getContext());
                table.db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                        .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        String user_id = querySnapshot.getDocuments().get(0).getId();
                                        // 🔥 Check if the same song exists for the user
                                        table.db.collection("SavedSongs")
                                                .whereEqualTo("USER_ID", user_id) // Ensure user does not have this song already
                                                .get()
                                                .addOnSuccessListener(songSnapshot -> {
                                                    if (songSnapshot.isEmpty()) {
                                                        ;
                                                    } else {
                                                        for (DocumentSnapshot documentSnapshot : songSnapshot.getDocuments()) {
                                                            table.remove_saved_song(email, documentSnapshot.getString("TITLE"));
                                                        }
                                                    }
                                                });
                                    } else {
                                        ;
                                    }
                        });
            }
        });
        //Play button functionality
        ImageButton btn2 = view.findViewById(R.id.play_all);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shuffle_on == false) {
                    SearchResponse.Track track = sessionManager.getSavedTracklist("TRACK_LIST_LIKED").get(0);
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    Random rand = new Random();
                    int index = rand.nextInt(sessionManager.getSavedTracklist("TRACK_LIST_LIKED").size());
                    SearchResponse.Track track = sessionManager.getSavedTracklist("TRACK_LIST_LIKED").get(index);
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
    //This function makes an API call using previous access token to search for random music
    //It does this based on the track_name entered
    private void search_track(String track_name, String Artist, String url) {
        String accesstoken = OnlinePlayerManager.getInstance().getAccess_token();
        if (accesstoken == null) {
            TextView text1 = view.findViewById(R.id.results);
            text1.setText("No internet connection, please try again.");
        } else {
            String randomQuery = "track:" + track_name + " artist:" + Artist;
            SpotifyApiService apiService = RetrofitClient.getClient(accesstoken).create(SpotifyApiService.class);
            Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");
            call.enqueue(new Callback<SearchResponse>() {
                @Override
                public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        //Getting track data and formatting
                        List<SearchResponse.Track> Tracks = response.body().getTracks().getItems();
                        List<String> tracks = get_track_names(Tracks);
                        List<String> urls = get_track_urls(Tracks);
                        //Getting indices of specified information
                        int i = tracks.indexOf(track_name+" by "+Artist);
                        int j = urls.indexOf(url);
                        //This counts the number of errors
                        int n = 0;
                        try {
                            if (Tracks.size() > 0) {
                                //Checking if it doesn't exist and performs j-index dependent adding
                                if (i == -1) {
                                    i = tracks.indexOf(track_name);
                                    ///Checking if both indices are equal
                                    if (i == j) {
                                        tracklist.add(Tracks.get(i));
                                    } else {
                                        //Otherwise checking if the url index holds the same title as the current title
                                        String title = tracks.get(j);
                                        if (title.equals(track_name+" by "+Artist) || title.equals(track_name)) {
                                            tracklist.add(Tracks.get(j));
                                        } else {
                                            ;
                                        }
                                    }
                                    //Otherwise, perform similar calculations but with i-index dependent adding
                                } else {
                                    if (i == j) {
                                        tracklist.add(Tracks.get(i));
                                    } else {
                                        String title = tracks.get(j);
                                        if (title.equals(track_name+" by "+Artist) || title.equals(track_name)) {
                                            tracklist.add(Tracks.get(j));
                                        } else {
                                            tracklist.add(Tracks.get(i));
                                        }
                                    }
                                }
                            } else {
                                n += 1;
                            }
                        } catch (Exception e) {
                            if (i == j && i == -1) {
                                tracklist.add(Tracks.get(i));
                            } else if (i != -1 && j == -1) {
                                tracklist.add(Tracks.get(i));
                            } else {
                                tracklist.add(Tracks.get(j));
                            }
                        }
                        sessionManager.save_Tracklist_liked(tracklist);
                        musicAdapter1.notifyDataSetChanged();
                        //Checking for more than One of the same track
                    } else if (response.code() == 401) { // Handle expired access token
                        String refresh = OnlinePlayerManager.getInstance().getRefresh_token();
                        TokenManager.getInstance().refreshAccessToken(refresh);
                    } else {
                        ;
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
    //This function replaces a tracklist with a list of track names
    public List<String> get_track_names(List<SearchResponse.Track> trackList) {
        // Use streams to map each Track object conditionally based on the presence of "(feat. "
        List<String> trackNames = trackList.stream()
                .map(track -> {
                    String name = track.getName();
                    if (name.contains("(feat. ")) {
                        // Perform some operation (e.g., returning the name or modify it)
                        return name; // You can modify this to your needs, such as processing the string
                    } else {
                        // Return something else if "(feat. " is not present
                        return name + " by "+track.getArtists().get(0).getName();
                    }
                })
                .collect(Collectors.toList());

        // Convert the List to an array
        return trackNames;
    }
    //This function gets tracklist with a list of album url's
    public List<String> get_track_urls(List<SearchResponse.Track> trackList) {
        // Use streams to map each Track object conditionally based on the availability of images
        List<String> track_urls = trackList.stream()
                .map(track -> {
                    // Check if album or image list is available
                    if (track.getAlbum() != null && track.getAlbum().getImages() != null && !track.getAlbum().getImages().isEmpty()) {
                        return track.getAlbum().getImages().get(0).getUrl(); // Return the first image URL
                    } else {
                        return ""; // Fallback in case album or images are missing
                    }
                })
                .filter(url -> !url.isEmpty()) // Optional: filter out any empty URLs
                .collect(Collectors.toList());
        return track_urls;
    }
    //This function gets the users liked songs in the database
    public void get_liked_songs() {
        SavedSongsFirestore table = new SavedSongsFirestore(getContext());
        sessionManager = new SessionManager(getContext());
        String email = sessionManager.getEmail();

        table.db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String userId = querySnapshot.getDocuments().get(0).getId();

                        table.db.collection("SavedSongs").whereEqualTo("USER_ID", userId).get()
                                .addOnSuccessListener(songSnapshot -> {
                                    TextView text = view.findViewById(R.id.x_liked);

                                    if (songSnapshot.isEmpty()) {
                                        text.setText("No liked songs");
                                    } else {
                                        List<DocumentSnapshot> documents = songSnapshot.getDocuments();
                                        for (DocumentSnapshot doc : documents) {
                                            String title = doc.getString("TITLE");
                                            String track_name = title;
                                            String artist = "";

                                            if (title != null && title.contains(" by ")) {
                                                String[] parts = title.split(" by ");
                                                if (parts.length == 2) {
                                                    track_name = parts[0];
                                                    artist = parts[1];
                                                }
                                            }

                                            try {
                                                search_track(track_name, artist, doc.getString("ALBUM_URL"));
                                            } catch (Exception e) {
                                                text.setText("No internet connection, please try again.");
                                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("Firebase", "Error checking song existence", e));
                    } else {
                        Log.e("Firebase", "User not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error retrieving user", e));
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
    private void stopPlayerService() {
        Intent intent = new Intent(requireContext(), PlayerService.class);
        requireContext().stopService(intent);
    }
}