package com.example.kzmusic;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaylistOverlay#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaylistOverlay extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    View view;
    String username;
    String email;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private SharedViewModel sharedViewModel;
    PlayerService playerService;
    Boolean isBound;
    ServiceConnection serviceConnection;
    private long last_position;
    private SessionManager sessionManager;
    private static final int REQUEST_CODE = 1;
    private RecyclerView recyclerView1;
    private MusicFileAdapter musicAdapter1;
    private List<MusicFile> playlist = new ArrayList<>();
    private List<MusicFile> musicFiles_original = new ArrayList<>();
    public PlaylistOverlay() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PlaylistOverlay.
     */
    // TODO: Rename and change types and number of parameters
    public static PlaylistOverlay newInstance(String param1, String param2) {
        PlaylistOverlay fragment = new PlaylistOverlay();
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
        view = inflater.inflate(R.layout.fragment_playlist_overlay, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.up_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        set_up_spotify_play();
        set_up_play_bar();
        sessionManager = new SessionManager(getContext());
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        SongQueue.getInstance().setCurrent_resource(R.layout.item_song2);
        recyclerView1 = view.findViewById(R.id.recycler_view_playlist_songs);
        recyclerView1.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter1 = new MusicFileAdapter(getContext(), playlist);
        recyclerView1.setAdapter(musicAdapter1);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_CODE);
        } else {
            //Loading music files into recycler view
            loadMusicFiles();
        }
        if (SongQueue.getInstance().current_playlist != null) {
            TextView playlist_name = view.findViewById(R.id.playlist_name);
            playlist_name.setText(SongQueue.getInstance().current_playlist);
            get_playlist_songs(SongQueue.getInstance().current_playlist);
            set_up_buttons();
        }
        if (SongQueue.getInstance().get_size() > 0 && SongQueue.getInstance().current_song != null) {
            set_up_skipping();
        }
        return view;
    }
    //This function sets up functionality for the remaining buttons
    public void set_up_buttons() {
        Button edit_btn = view.findViewById(R.id.edit_btn);
        edit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment edit_page = new EditPlaylist();
                FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, edit_page);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
        ImageView edit_icon = view.findViewById(R.id.edit_icon);
        edit_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment edit_page = new EditPlaylist();
                FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, edit_page);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
        Button play_all_btn = view.findViewById(R.id.play_all_btn);
        if (SongQueue.getInstance().current_playlist.equals(SongQueue.getInstance().playing_playlist)) {
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause);
            play_all_btn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            SongQueue.getInstance().setPlaying_playlist(SongQueue.getInstance().current_playlist);
        }
        play_all_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SongQueue.getInstance().setSong_list(playlist);
                MusicFile track = playlist.get(0);
                if (track != null) {
                    open_overlay(track, 0);
                }
                Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause);
                play_all_btn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                SongQueue.getInstance().setPlaying_playlist(SongQueue.getInstance().current_playlist);
            }
        });
        Button shuffle_btn = view.findViewById(R.id.shuffle_btn);
        if (SongQueue.getInstance().shuffle_on == true) {
            //Setting repeat mode on and replacing icon
            SongQueue.getInstance().setShuffle_on(true);
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_shuffle_on);
            shuffle_btn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        } else {
            //Setting repeat mode off and replacing icon
            SongQueue.getInstance().setShuffle_on(false);
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_shuffle);
            shuffle_btn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        shuffle_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SongQueue.getInstance().setSong_list(playlist);
                if (SongQueue.getInstance().shuffle_on == true) {
                    //Setting repeat mode on and replacing icon
                    SongQueue.getInstance().setShuffle_on(false);
                    Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_shuffle);
                    shuffle_btn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

                } else {
                    //Setting repeat mode off and replacing icon
                    SongQueue.getInstance().setShuffle_on(true);
                    Random rand = new Random();
                    int index = rand.nextInt(playlist.size());
                    MusicFile track = playlist.get(index);
                    if (track != null) {
                        open_overlay(track, index);
                    }
                    SongQueue.getInstance().setPlaying_playlist(SongQueue.getInstance().current_playlist);
                    Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_shuffle_on);
                    shuffle_btn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                }
            }
        });
    }
    //This function opens the playback handling overlay
    //Whilst sending necessary data over
    public void open_overlay(MusicFile musicFile, int position) {
        Fragment media_page = new MediaOverlay();
        //Adding song to queue
        SongQueue.getInstance().addSong(musicFile);
        SongQueue.getInstance().setPosition(position);
        //Opening fragment
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    //This function gets playlist songs in database
    public void get_playlist_songs(String playlist_title) {
        ImageView art = view.findViewById(R.id.album_art);
        SessionManager sessionManager = new SessionManager(getContext());
        String email = sessionManager.getEmail();
        PlaylistDao playlistDao = AppDatabase.getDatabase(getContext()).playlistDao();
        PlaylistSongDao playlistSongDao = AppDatabase.getDatabase(getContext()).playlistSongDao();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            int playlistId = playlistDao.getPlaylistIdByEmailAndTitle(email, playlist_title);
            List<String> songs = playlistSongDao.get_playlist_songs(email, playlistId);

            if (songs == null || songs.isEmpty()) {
                Log.d("RoomDB", "✅ Empty Playlist " + playlistId);
                return;
            }

            // Build a set of current song names for fast lookup
            Set<String> existingNames = new HashSet<>();
            for (MusicFile mf : playlist) {
                existingNames.add(mf.getName());
            }

            List<MusicFile> toAdd = new ArrayList<>();
            for (MusicFile musicFile : musicFiles_original) {
                String name = musicFile.getName();
                if (songs.contains(name) && !existingNames.contains(name)) {
                    toAdd.add(musicFile);
                    existingNames.add(name); // Update the set to prevent duplicates in toAdd itself
                }
            }

            String url = playlistDao.getUrl(email, playlist_title);

            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    playlist.addAll(toAdd); // Guaranteed no duplicates by name

                    if (url == null || url.isEmpty()) {
                        art.setImageResource(R.drawable.logo);
                    } else {
                        art.setImageURI(Uri.parse(url));
                    }

                    musicAdapter1.notifyDataSetChanged();
                    Log.d("RoomDB", "🔁 Displaying playlist " + playlistId);
                });
            }
        });
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
                    get_playlist_songs(SongQueue.getInstance().current_playlist);
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

        } else {
            //Adding song to queue
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