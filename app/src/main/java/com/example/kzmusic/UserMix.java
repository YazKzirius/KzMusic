package com.example.kzmusic;

//Imports
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.media.session.MediaButtonReceiver;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
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

    public UserMix(String token) {
        // Required empty public constructor
        this.access_token = token;
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
        UserMix fragment = new UserMix(param1);
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
        ic_down = view.findViewById(R.id.down_button);
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
                if (PlayerManager.getInstance().get_size() > 0) {
                    PlayerManager.getInstance().current_player.pause();
                    SpotifyPlayerLife.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    SpotifyPlayerLife.getInstance().setCurrent_track(track);
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
                    display_generated_music(access_token, query);
                }
            } catch (Exception e) {
                text.setText("No media files, please update library.");
            }
        } else {
            musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_MIX"));
        }
        set_up_spotify_play();
        set_up_play_bar();
        set_up_refresh();
        if (SongQueue.getInstance().get_size() > 0) {
            set_up_skipping();
            last_position = PlayerManager.getInstance().current_player.getCurrentPosition();
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
                        display_generated_music(access_token, query);
                    }
                } catch (Exception e) {
                    TextView text = view.findViewById(R.id.made_for_user);
                    text.setText("No media files, please update library.");
                }
            }
        });
    }
    //This function checks if all songs in view are liked
    public Boolean all_liked() {
        for (SearchResponse.Track track : sessionManager.getSavedTracklist("TRACK_LIST_MIX")) {
            UsersTable table = new UsersTable(getContext());
            table.open();
            String email = sessionManager.getEmail();
            String title = track.getName()+" by "+track.getArtists().get(0).getName();
            if (table.song_liked(title, email) == false) {
                return false;
            }
        }
        return true;
    }
    public void update_total_duration() {
        long duration = PlayerManager.getInstance().current_player.getCurrentPosition() - last_position;
        String display_title = format_title(SongQueue.getInstance().current_song.getName()) + " by " + SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", ");
        //Updating song duration database
        SessionManager sessionManager = new SessionManager(getContext());
        String email = sessionManager.getEmail();
        UsersTable table = new UsersTable(getContext());
        table.open();
        table.update_song_duration(email, display_title, (int) duration/1000);
        table.close();
    }
    //This function sets up playback buttons at top
    public void set_up_playback_buttons() {
        //Session class
        sessionManager = new SessionManager(getContext());
        //Setting up liked all button
        ImageButton btn1 = view.findViewById(R.id.like_all);
        btn1.setImageResource(R.drawable.ic_liked_off);
        if (all_liked() == true) {
            btn1.setImageResource(R.drawable.ic_liked);
        }
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                liked_on = !liked_on;
                //If liked all button on, like all songs in recycler view and display liked icon
                if (liked_on == true) {
                    btn1.setImageResource(R.drawable.ic_liked);
                    for (SearchResponse.Track track : sessionManager.getSavedTracklist("TRACK_LIST_MIX")) {
                        UsersTable table = new UsersTable(getContext());
                        table.open();
                        String email = sessionManager.getEmail();
                        String title = track.getName()+" by "+track.getArtists().get(0).getName();
                        String url = track.getAlbum().getImages().get(0).getUrl();
                        if (table.song_liked(title, email) == true) {
                            ;
                        } else {
                            table.add_liked_song(email, title, url);
                        }
                        musicAdapter.clear_tracks();
                        musicAdapter.updateTracks(sessionManager.getSavedTracklist("TRACK_LIST_MIX"));

                    }
                    //Otherwise, unlike all songs and display unliked icon
                } else {
                    btn1.setImageResource(R.drawable.ic_liked_off);
                    for (SearchResponse.Track track : sessionManager.getSavedTracklist("TRACK_LIST_MIX")) {
                        UsersTable table = new UsersTable(getContext());
                        table.open();
                        String email = sessionManager.getEmail();
                        String title = track.getName()+" by "+track.getArtists().get(0).getName();
                        table.remove_liked(email, title);
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
                    SpotifyPlayerLife.getInstance().setCurrent_track(track);
                    open_spotify_overlay();
                } else {
                    Random rand = new Random();
                    int index = rand.nextInt(sessionManager.getSavedTracklist("TRACK_LIST_MIX").size());
                    SearchResponse.Track track = sessionManager.getSavedTracklist("TRACK_LIST_MIX").get(index);
                    SpotifyPlayerLife.getInstance().setCurrent_track(track);
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
    public void display_generated_music(String token, String artist) {
        access_token = token;
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
        if (SpotifyPlayerLife.getInstance().mSpotifyAppRemote != null) {
            SpotifyPlayerLife.getInstance().mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {
                    if (playerState.isPaused) {
                        ;
                    } else {
                        if (PlayerManager.getInstance().current_player != null) {
                            PlayerManager.getInstance().current_player.pause();
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
        update_total_duration();
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