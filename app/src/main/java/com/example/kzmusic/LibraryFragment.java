package com.example.kzmusic;

//Imports
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LibraryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
//This implements the user library fragment
public class LibraryFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    View view;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    ExoPlayer exo_player;
    private EnvironmentalReverb reverb;
    int session_id;
    private SharedViewModel sharedViewModel;
    PlayerService playerService;
    Boolean isBound;
    ServiceConnection serviceConnection;
    private long last_position;
    private List<MusicFile> musicFiles_original = new ArrayList<>();
    private RecyclerView recyclerView1;
    private MusicFileAdapter musicAdapter1;
    private List<MusicFile> play_history = new ArrayList<>();
    private SessionManager sessionManager;
    private LinearLayout local;
    private static final int REQUEST_CODE = 1;
    private RecyclerView recyclerView2;
    private PlaylistAdapter playlistAdapter2;
    private List<Playlist> playlists = new ArrayList<>();

    public LibraryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LibraryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LibraryFragment newInstance(String param1, String param2) {
        LibraryFragment fragment = new LibraryFragment();
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
        view = inflater.inflate(R.layout.fragment_library, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.up_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        local = view.findViewById(R.id.local_library_row);
        //First recycler view
        SongQueue.getInstance().setCurrent_resource(R.layout.item_song2);
        recyclerView1 = view.findViewById(R.id.recycler_view_history);
        recyclerView1.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        musicAdapter1 = new MusicFileAdapter(getContext(), play_history);
        recyclerView1.setAdapter(musicAdapter1);
        recyclerView2 = view.findViewById(R.id.recycler_view_playlist);
        recyclerView2.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        playlistAdapter2 = new PlaylistAdapter(getContext(), playlists);
        recyclerView2.setAdapter(playlistAdapter2);
        sessionManager = new SessionManager(getContext());
        //Setting up bottom playback navigator
        set_up_spotify_play();
        set_up_play_bar();
        if (SongQueue.getInstance().get_size() > 0 && SongQueue.getInstance().current_song != null) {
            set_up_skipping();
        } else {
            ;
        }
        set_up_library();
        if (SongQueue.getInstance().history == null) {
            ;
        } else {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_CODE);
            } else {
                musicFiles_original.clear();
                //Loading music files into recycler view
                loadMusicFiles();
            }
            TextView text = view.findViewById(R.id.no_tracks_label);
            text.setText(musicFiles_original.size()+" tracks");
            update_history();
        }
        //Adding current playlists
        loadPlaylists();
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ;
    }
    //This function updates play history
    public void update_history() {
        List<MusicFile> history = new ArrayList<>();
        List<String> trackNames = musicFiles_original.stream()
                .map(track -> {
                    String name = track.getName();
                    return name;
                })
                .collect(Collectors.toList());
        for (MusicFile musicFile : SongQueue.getInstance().history) {
            if (trackNames.contains(musicFile.getName())) {
                history.add(musicFile);
            } else {
                ;
            }
        }
        Collections.reverse(history);
        play_history.clear();
        play_history.addAll(history);
        musicAdapter1.notifyDataSetChanged();
    }
    private void loadPlaylists() {
        PlaylistDao playlistDao = AppDatabase.getDatabase(requireContext()).playlistDao();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            String email = sessionManager.getEmail();
            List<Playlist> loadedPlaylists = playlistDao.getAllPlaylists(email);

            // Delay UI update until Fragment is attached and view is still valid
            if (isAdded() && getActivity() != null && getView() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) { // double-check in case it detached meanwhile
                        playlists.clear();
                        playlists.addAll(loadedPlaylists);
                        playlistAdapter2.notifyDataSetChanged();
                    }
                });
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
                    update_history();
                    set_up_play_bar();
                }
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
            int count = 1;
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
                    musicFiles_original.add(musicFile);
                }
            }
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
            SongQueue.getInstance().addSong(file);
            SongQueue.getInstance().setPosition(position);
            Fragment media_page = new MediaOverlay();
            if (playerService != null) {
                playerService.updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
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
    //This function sets up library button functionality
    public void set_up_library() {
        ImageView button = view.findViewById(R.id.library_btn);
        ImageButton button2 = view.findViewById(R.id.add_playlist);
        //Library button functionality
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment music_page = new UserMusic();
                FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, music_page);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
        local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment music_page = new UserMusic();
                FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, music_page);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment music_page = new PlaylistCreation();
                FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, music_page);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
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