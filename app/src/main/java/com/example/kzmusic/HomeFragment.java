package com.example.kzmusic;

//Imports
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private List<SearchResponse.Track> trackList = new ArrayList<>();
    private static final String TRACK_LIST_KEY = "track_list";
    private MusicAdapter musicAdapter;
    String accesstoken;
    SessionManager sessionManager;
    String email;
    String username;
    SpotifyAuthService authService;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view1);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicAdapter(trackList, getContext());
        recyclerView.setAdapter(musicAdapter);
        sessionManager = new SessionManager(getContext());
        authService = new SpotifyAuthService();
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        TextView text = view.findViewById(R.id.made_for_x);
        text.setText("Made for "+username);
        //Opening getting access token from Spotify API AUTH
        if (getArguments() != null) {
            accesstoken = getArguments().getString("Token");
        }
        display_music(accesstoken);
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);;
    }
    //This function displays gets the music based on token validation
    public void display_music(String token) {
        //Performing access token validation, if token expires it gets a new one
        if (token == null) {
            authService.getAccessToken(new SpotifyAuthService.Callback<String>() {
                @Override
                public void onSuccess(String new_token) {
                    searchRandomMusic(new_token);
                }
                @Override
                public void onFailure(Throwable t) {
                    Intent intent = new Intent(getContext(), GetStarted.class);
                    startActivity(intent);
                }
            });
        } else {
            searchRandomMusic(token);
        }

    }
    //This function searches for random music using API queries and updates the current tracklist
    private void searchRandomMusic(String token) {
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
}