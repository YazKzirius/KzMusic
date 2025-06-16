package com.example.kzmusic;

//Imports
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.provider.MediaStore;
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

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserMix#newInstance} factory method to
 * create an instance of this fragment.
 */
//This class implements the User mix fragment which uses Machine learning
//To generate Music based on the users current taste and top artists
public class UserMix extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    //Page attributes
    private  List<SearchResponse.Track> trackList = new ArrayList<>();
    private List<SearchResponse.Track> tracklist = new ArrayList<>();
    private List<MusicFile> musicFiles = new ArrayList<>();
    RecyclerView recyclerView;
    String access_token;
    View view;
    MusicAdapter musicAdapter;
    SessionManager sessionManager;
    String email;
    String username;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private SharedViewModel sharedViewModel;
    PlayerService playerService;
    Boolean isBound;
    ServiceConnection serviceConnection;
    Boolean liked_on = false;
    Boolean shuffle_on = false;
    private long last_position;

    public UserMix() {
        // Required empty public constructor
        ;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserMix.
     */
    // TODO: Rename and change types and number of parameters
    public static UserMix newInstance(String param1, String param2) {
        UserMix fragment = new UserMix();
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
        view = inflater.inflate(R.layout.fragment_user_mix, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.up_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        sessionManager = new SessionManager(getContext());
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        recyclerView=view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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
        recyclerView.setAdapter(musicAdapter);
        //Load music files
        loadMusicFiles();
        //Setting text
        TextView text = view.findViewById(R.id.made_for_user);
        text.setText("Suggestsed mix for "+username);
        //If saved list is empty, display new tracks
        if (sessionManager.getSavedTracklist("TRACK_LIST_MIX").size() == 0) {
            try {
                String[] randomQueries = generate_top_artists(musicFiles);
                for (String query : randomQueries) {
                    display_generated_music(query);
                }
            } catch (Exception e) {
                text.setText("No media files, please update library.");
            }
            //Setting up liked all button
            ImageButton btn1 = view.findViewById(R.id.like_all);
            btn1.setImageResource(R.drawable.ic_liked_off);
        } else {
            musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_MIX"));
        }
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
    //This function loads User music audio files from personal directory
    private void loadMusicFiles() {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = getContext().getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                null
        )) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                //Getting music information
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                String artist = cursor.getString(artistColumn);
                String data = cursor.getString(dataColumn);
                long albumId = cursor.getLong(albumIdColumn);
                //Defining music file
                MusicFile musicFile = new MusicFile(id, name, artist, data, albumId);
                //Filtering out music from short sounds and voice recordings
                if (artist.equals("Voice Recorder")) {
                    ;
                } else if (artist.equals("<unknown>")) {
                    ;
                } else {
                    musicFiles.add(musicFile);
                }
            }
        }
    }
    //This function sets up refresh button
    public void set_up_refresh() {
        Button refresh_btn = view.findViewById(R.id.refresh_btn);
        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    musicAdapter.clear_tracks();
                    String[] randomQueries = generate_top_artists(musicFiles);
                    for (String query : randomQueries) {
                        display_generated_music(query);
                    }
                } catch (Exception e) {
                    TextView text = view.findViewById(R.id.made_for_user);
                    text.setText("No media files, please update library.");
                }
            }
        });
    }
    //This function checks if all songs in view are liked
    public void all_liked(OnSuccessListener<Boolean> callback) {
        SavedSongsFirestore table = new SavedSongsFirestore(getContext());
        String email = sessionManager.getEmail();
        List<SearchResponse.Track> trackList = sessionManager.getSavedTracklist("TRACK_LIST_MIX");

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
                SavedSongsFirestore table = new SavedSongsFirestore(getContext());
                //If liked all button on, like all songs in recycler view and display liked icon
                if (liked_on == true) {
                    btn1.setImageResource(R.drawable.ic_liked);
                    for (SearchResponse.Track track : sessionManager.getSavedTracklist("TRACK_LIST_MIX")) {
                        String title = track.getName()+" by "+track.getArtists().get(0).getName();
                        String url = track.getAlbum().getImages().get(0).getUrl();
                        table.save_new_song(email, title, url);
                        musicAdapter.clear_tracks();
                        musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_MIX"));

                    }
                    //Otherwise, unlike all songs and display unliked icon
                } else {
                    btn1.setImageResource(R.drawable.ic_liked_off);
                    for (SearchResponse.Track track : sessionManager.getSavedTracklist("TRACK_LIST_MIX")) {
                        String title = track.getName()+" by "+track.getArtists().get(0).getName();
                        String url = track.getAlbum().getImages().get(0).getUrl();
                        table.remove_saved_song(email, title, url);
                        musicAdapter.clear_tracks();
                        musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_MIX"));

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
                    SearchResponse.Track track = sessionManager.getSavedTracklist("TRACK_LIST_MIX").get(0);
                    OnlinePlayerManager.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    Random rand = new Random();
                    int index = rand.nextInt(sessionManager.getSavedTracklist("TRACK_LIST_MIX").size());
                    SearchResponse.Track track = sessionManager.getSavedTracklist("TRACK_LIST_MIX").get(index);
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
    //This function searches for random music using API queries and updates the current tracklist
    public void display_generated_music(String artist) {
        access_token = OnlinePlayerManager.getInstance().getAccess_token();
        if (access_token == null) {
            TextView text1 = view.findViewById(R.id.made_for_user);
            text1.setText("No internet connection, please try again.");
        } else {
            String randomQuery = "artist: " + artist;
            SpotifyApiService apiService = RetrofitClient.getClient(access_token).create(SpotifyApiService.class);
            Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");
            call.enqueue(new Callback<SearchResponse>() {
                @Override
                public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        musicAdapter.updateTracks(response.body().getTracks().getItems().subList(0, 4));
                        tracklist.addAll(response.body().getTracks().getItems().subList(0, 4));
                        if (tracklist.size() == 40) {
                            sessionManager.save_Tracklist_mix(tracklist);
                        }
                    } else if (response.code() == 401) { // Handle expired access token
                        ;
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
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(getContext(), target);
        startActivity(intent);
    }
    //This function gets the User's top 10 artists and using and algorithm
    //It then generates music based on that information
    public String[] generate_top_artists(List<MusicFile> tracklist) {
        String[] top_artists = new String[10];
        //Mapping get artist function to each track in list
        List<String> Artists = tracklist.stream().map(MusicFile::getArtist).collect(Collectors.toList());
        //Fixing Artist formatting, so there is a full list of artists
        List<String> New_artists = new ArrayList<>();
        //Checking for multiple artists
        for (String artist : Artists) {
            if (artist.contains(", ") == true) {
                String[] list = artist.split(", ");
                for (String element : list) {
                    New_artists.add(element);
                }
            } else {
                New_artists.add(artist);
            }
        }
        Set<String> Artist_set = new HashSet<>(New_artists);
        List<String> Artist_list = new ArrayList<>(Artist_set);
        //Frequency dict hashmap
        Map<String, Integer> frequency_dict = new HashMap<>();
        for (String artist : Artist_list) {
            frequency_dict.put(artist, count(New_artists, artist));
        }
        //Creating string occurrence comparator
        Comparator<String> occurrenceComparator = (s1, s2) -> Integer.compare(frequency_dict.get(s2), frequency_dict.get(s1));
        Artist_list.sort(occurrenceComparator);
        for (int i = 0; i < 10; i++) {
            top_artists[i] = Artist_list.get(i);
        }
        return top_artists;
    }
    //This function counts the number of an element of a list
    public int count(List<String> list, String element) {
        int count = 0;
        for (String string : list) {
            if (string.equalsIgnoreCase(element)) {
                count += 1;
            } else {
                ;
            }
        }
        return count;
    }
    //This function opens Spotify player overlay
    public void open_spotify_overlay() {
        Fragment spotify_overlay = new SpotifyOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, spotify_overlay);
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