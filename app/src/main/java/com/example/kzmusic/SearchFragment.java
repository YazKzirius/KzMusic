package com.example.kzmusic;

//Imports
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.inputmethod.EditorInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

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
        View view =  inflater.inflate(R.layout.fragment_search, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view3);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicAdapter(trackList, getContext(), new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchResponse.Track track) {
                Toast.makeText(getContext(), "Playing Songs Similar to: "+track.getName(), Toast.LENGTH_SHORT).show();
                //Stopping all players, so no playback overlap
                PlayerManager.getInstance().stopAllPlayers();
                play_track(track.getUri());
            }
        });
        recyclerView.setAdapter(musicAdapter);
        if (getArguments() != null) {
            accesstoken = getArguments().getString("Token");
        }
        TextView View = view.findViewById(R.id.results);
        EditText search = view.findViewById(R.id.search_input);
        View.setText("Search results:");
        display_random(accesstoken);
        Button search_button = view.findViewById(R.id.search_button);
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Searching for random tracks based on the name input
                String input = search.getText().toString();
                if (input.equals("")) {
                    display_random(accesstoken);
                    View.setText("Search results:");
                } else {
                    //Displaying results
                    search_track(input, accesstoken);
                    View.setText("Search results:");
                    display_random(accesstoken);
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
                        display_random(accesstoken);
                    } else {
                        search_track(input, accesstoken);
                    }
                    View.setText("Search results:");
                    return true; // Return true to indicate the event was handled
                }
                return false; // Return false if the event is not handled
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ;
    }
    //This function makes an API call using previous access token to search for random music
    //It does this based on the track_name entered
    private void search_track(String track_name, String token) {
        accesstoken = token;
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
                } else {
                    Intent intent = new Intent(getContext(), GetStarted.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(getContext(), "API call failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    //This function gets random music based on catergory
    //It does this before the user chooses to search for a random track
    private void display_random(String token) {
        accesstoken = token;
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
                } else {
                    Intent intent = new Intent(getContext(), GetStarted.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(getContext(), "API call failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    //These functions authenticate Spotify remote use
    @Override
    public void onStart() {
        super.onStart();
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(getContext(), connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("SpotifyAppRemote", "Connected");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("SpotifyAppRemote", throwable.getMessage(), throwable);
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }
    //These functions handle album playback
    public void play_track(String uri) {
        if (mSpotifyAppRemote != null) {
            PlayerApi player = mSpotifyAppRemote.getPlayerApi();
            player.play(uri);
            player.subscribeToPlayerState()
                    .setEventCallback(new Subscription.EventCallback<PlayerState>() {
                        @Override
                        public void onEvent(PlayerState playerState) {
                            final Track track = playerState.track;
                        }
                    });
        }
    }
}