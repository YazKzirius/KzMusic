package com.example.kzmusic;

//Imports
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

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
    ImageButton btnPlayPause;
    RelativeLayout playback_bar;
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
        btnPlayPause = view.findViewById(R.id.play_pause_button);
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
                    btnPlayPause.setImageResource(R.drawable.ic_play);
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
        String[] randomQueries = generate_top_artists(musicFiles);
        for (String query : randomQueries) {
            display_generated_music(access_token, query);
        }
        set_up_spotify_play();
        set_up_play_bar();
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
    //This function searches for random music using API queries and updates the current tracklist
    public void display_generated_music(String token, String artist) {
        access_token = token;
        String randomQuery = artist;
        TextView text = view.findViewById(R.id.made_for_user);
        text.setText("Suggestsed mix for "+username);
        SpotifyApiService apiService = RetrofitClient.getClient(access_token).create(SpotifyApiService.class);
        Call<SearchResponse> call = apiService.searchTracks(randomQuery, "track");
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    musicAdapter.updateTracks(response.body().getTracks().getItems());
                } else {
                    ;
                }
            }
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(getContext(), "API call failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                        btnPlayPause.setImageResource(R.drawable.ic_play);
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
            Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
            Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(song.getAlbumId()));
            Glide.with(getContext()).asBitmap().load(album_uri).circleCrop().into(art);
            title.setText("Now playing "+song.getName().replace("[SPOTIFY-DOWNLOADER.COM] ", "").replace(".mp3", ""));
            Artist.setText(song.getArtist());
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
            //Implementing pause button functionality
            if (PlayerManager.getInstance().get_size() > 0) {
                if (PlayerManager.getInstance().current_player.isPlaying()) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    if (SpotifyPlayerLife.getInstance().mSpotifyAppRemote != null) {
                        SpotifyPlayerLife.getInstance().pause_playback();
                    }
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
            btnPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Checking if they're is already a song currently playing
                    if (PlayerManager.getInstance().get_size() > 0) {
                        if (PlayerManager.getInstance().current_player.isPlaying()) {
                            PlayerManager.getInstance().current_player.pause();
                            btnPlayPause.setImageResource(R.drawable.ic_play);
                        } else {
                            if (SpotifyPlayerLife.getInstance().mSpotifyAppRemote != null) {
                                SpotifyPlayerLife.getInstance().pause_playback();
                            }
                            PlayerManager.getInstance().current_player.play();
                            btnPlayPause.setImageResource(R.drawable.ic_pause);
                        }
                    } else {
                        ;
                    }


                }
            });
        }
    }
    //This function opens a new song overlay
    public void open_new_overlay(MusicFile file, int position) {
        //Adding song to queue
        SongQueue.getInstance().addSong(file);
        SongQueue.getInstance().setPosition(position);
        Fragment media_page = new MediaOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
    }
}