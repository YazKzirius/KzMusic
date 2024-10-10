package com.example.kzmusic;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Top10Songs#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Top10Songs extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int REQUEST_CODE = 1;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    List<MusicFile> musicFiles = new ArrayList<>();
    List<MusicFile> top_5_songs = new ArrayList<>();
    List<SearchResponse.Track> top_5_vids = new ArrayList<>();
    String token;
    String email;
    String username;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private SessionManager sessionManager;
    private RecyclerView recyclerView1;
    private MusicFileAdapter musicAdapter1;
    private RecyclerView recyclerView2;
    private MusicAdapter musicAdapter2;
    private long last_position;
    private ServiceConnection serviceConnection;
    private PlayerService playerService;
    private boolean isBound;
    private SharedViewModel sharedViewModel;
    View view;
    private List<SearchResponse.Track> tracklist = new ArrayList<>();
    private List<SearchResponse.Track> sorted_tracklist = new ArrayList<>();
    public Top10Songs() {
        // Required empty public constructor
        ;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Top10Songs.
     */
    // TODO: Rename and change types and number of parameters
    public static Top10Songs newInstance(String param1, String param2) {
        Top10Songs fragment = new Top10Songs();
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
       view = inflater.inflate(R.layout.fragment_top10_songs, container, false);
       art = view.findViewById(R.id.current_song_art);
       title = view.findViewById(R.id.current_song_title);
       Artist = view.findViewById(R.id.current_song_artist);
       ic_down = view.findViewById(R.id.up_button);
       playback_bar = view.findViewById(R.id.playback_bar);
       sessionManager = new SessionManager(getContext());
       username = sessionManager.getUsername();
       email = sessionManager.getEmail();
       //Getting data in view
       get_top_5_songs();
       //Getting top 5 videos
        recyclerView2 = view.findViewById(R.id.recycler_view2);
        recyclerView2.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter2 = new MusicAdapter(sorted_tracklist, getContext(), new MusicAdapter.OnItemClickListener() {
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
        recyclerView2.setAdapter(musicAdapter2);
        get_song_vids();
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
    //This function gets the user's top 5 songs
    public void get_top_5_songs() {
        recyclerView1 = view.findViewById(R.id.recycler_view1);
        recyclerView1.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter1 = new MusicFileAdapter(getContext(), top_5_songs);
        recyclerView1.setAdapter(musicAdapter1);
        loadMusicFiles();
        if (musicFiles.size() > 0) {
            SongQueue.getInstance().setCurrent_resource(R.layout.item_song2);
            UsersTable table = new UsersTable(getContext());
            table.open();
            Cursor cursor = table.display_most_played(email);
            int count = 0;
            while (cursor.moveToNext() && count < 5) {
                String title = cursor.getString(cursor.getColumnIndex("TITLE"));
                if (title.contains("(Official Music Video)")) {
                    ;
                } else {
                    top_5_songs.add(get_music_file(title));
                    musicAdapter1.notifyDataSetChanged();
                    count += 1;
                }
            }
            SongQueue.getInstance().setSong_list(top_5_songs);
        } else {
            ;
        }
    }

    //This function gets music files by specific name
    public MusicFile get_music_file(String name) {
        List<String> track_names = musicFiles.stream().map(track -> {
            String track_name = format_title(track.getName()) + " by "+track.getArtist().replaceAll("/", ", ");
            return track_name;
        }).collect(Collectors.toList());
        int index = track_names.indexOf(name);
        return musicFiles.get(index);
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
            SongQueue.getInstance().setSong_list(musicFiles);
        }
    }
    //This function makes an API call using previous access token to search for random music
    //It does this based on the track_name entered
    private void search_track(String track_name, String Artist) {
        String refresh = OnlinePlayerManager.getInstance().getRefresh_token();
        TokenManager.getInstance().refreshAccessToken(refresh);
        String accesstoken = OnlinePlayerManager.getInstance().getAccess_token();
        if (accesstoken == null)  {
            ;
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
                        //Getting indices of specified information
                        int i = tracks.indexOf(track_name+" by "+Artist);
                        //Checking if it doesn't exist and performs j-index dependent adding
                        if (i == -1) {
                            i = tracks.indexOf(track_name);
                            ///Checking if both indices are equal
                            if (i != -1) {
                                tracklist.add(Tracks.get(i));
                            } else {
                                //Otherwise get first element of tracklist
                                tracklist.add(Tracks.get(0));
                            }
                        } else {
                            //Otherwise get first element of tracklist
                            tracklist.add(Tracks.get(i));
                        }
                        if (tracklist.size() == 5) {
                            sort_track_list();
                        }
                        //Checking for more than One of the same track
                    } else {
                        ;
                    }

                }
                @Override
                public void onFailure(Call<SearchResponse> call, Throwable t) {
                    ;
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
    //This function gets the total number of music vids
    public int getN_vids() {
        int n_vids= 0;
        UsersTable table = new UsersTable(getContext());
        table.open();
        Cursor cursor = table.display_most_played(email);
        while (cursor.moveToNext()) {
            String title = cursor.getString(cursor.getColumnIndex("TITLE"));
            if (title.contains(" (Official Music Video")) {
                n_vids += 1;
            }
        }
        table.close();
        return n_vids;
    }
    //This function sorts tracklist by descending order of most played
    public void sort_track_list() {
        OnlinePlayerManager.getInstance().setCurrent_context(getContext());
        sorted_tracklist = tracklist.stream()
                .sorted(Comparator.comparingInt(SearchResponse.Track::get_n_times).reversed())
                .collect(Collectors.toList());
        musicAdapter2.updateTracks(sorted_tracklist);
    }
    //This function gets your top 5 most played videos
    public void get_song_vids() {
        if (getN_vids() > 0) {
            try {
                UsersTable table = new UsersTable(getContext());
                table.open();
                Cursor cursor = table.display_most_played(email);
                while (cursor.moveToNext()) {
                    String title = cursor.getString(cursor.getColumnIndex("TITLE"));
                    if (title.contains("(Official Music Video")) {
                        title = title.replace(" (Official Music Video)", "");
                        String track_name = "";
                        String artist = "";
                        if (title.split(" by ").length == 2) {
                            track_name = title.split(" by ")[0];
                            artist = title.split(" by ")[1];
                            search_track(track_name, artist);
                        }
                    } else {
                        ;
                    }
                }
                table.close();
            } catch (Exception e) {
                ;
            }
        } else {
            ;
        }

    }
    //This function updates the total song duration attribute in databse
    public void update_total_duration() {
        long duration = OfflinePlayerManager.getInstance().current_player.getCurrentPosition() - last_position;
        String display_title = format_title(SongQueue.getInstance().current_song.getName()) + " by " + SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", ");
        //Updating song duration database
        SessionManager sessionManager = new SessionManager(getContext());
        String email = sessionManager.getEmail();
        UsersTable table = new UsersTable(getContext());
        table.open();
        table.update_song_duration(email, display_title, (int) (duration/(1000 * SongQueue.getInstance().speed)));
        table.close();
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
        update_total_duration();
        SongQueue.getInstance().addSong(file);
        SongQueue.getInstance().setPosition(position);
        Fragment media_page = new MediaOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
    }
    //This function opens Spotify player overlay
    public void open_spotify_overlay() {
        Fragment spotify_overlay = new SpotifyOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, spotify_overlay);
        fragmentTransaction.commit();
    }
    private void stopPlayerService() {
        Intent intent = new Intent(requireContext(), PlayerService.class);
        requireContext().stopService(intent);
    }
}