package com.example.kzmusic;

//Imports
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//This class manages user login is sessions so user can stay logged into app until log out
public class SessionManager {

    private SharedPreferences sharedPreferences;
    private SharedPreferences sharedPreferences2;
    private SharedPreferences.Editor editor;
    private SharedPreferences.Editor editor2;
    private Context context;
    private static final String PREF2_NAME = "TracklistSession";
    private static final String PREF_NAME = "loginSession";
    private static final String KEY_IS_LOGGEDIN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences2 = context.getSharedPreferences(PREF2_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor2 = sharedPreferences2.edit();
    }

    public void createLoginSession(String username, String email) {
        editor.putBoolean(KEY_IS_LOGGEDIN, true);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.commit();
    }

    //This function saves generated tracklist for radio page
    public void save_Tracklist_radio(List<SearchResponse.Track> tracklist) {
        // Convert list to JSON
        Gson gson = new Gson();
        String jsonTrackList = gson.toJson(tracklist);
        // Save in SharedPreferences
        editor2.putString("TRACK_LIST_RADIO", jsonTrackList);
        editor2.commit();
    }
    //This function saves generated tracklist for mix page
    public void save_Tracklist_mix(List<SearchResponse.Track> tracklist) {
        // Convert list to JSON
        Gson gson = new Gson();
        String jsonTrackList = gson.toJson(tracklist);
        // Save in SharedPreferences
        editor2.putString("TRACK_LIST_MIX", jsonTrackList);
        editor2.commit();
    }
    //This function saves generated tracklist for liked songs page
    public void save_Tracklist_liked(List<SearchResponse.Track> tracklist) {
        // Convert list to JSON
        Gson gson = new Gson();
        String jsonTrackList = gson.toJson(tracklist);
        // Save in SharedPreferences
        editor2.putString("TRACK_LIST_LIKED", jsonTrackList);
        editor2.commit();
    }
    //This function gets the tracklist for a specific name
    public List<SearchResponse.Track> getSavedTracklist(String name) {
        String jsonTrackList = sharedPreferences2.getString(name, null);
        if (jsonTrackList != null) {
            Gson gson = new Gson();
            SearchResponse.Track[] trackArray = gson.fromJson(jsonTrackList, SearchResponse.Track[].class);
            return Arrays.asList(trackArray);  // Convert array back to list
        }
        return new ArrayList<>();  // Return an empty list if nothing is saved
    }
    //This function clears saved tracklist
    public void clear_tracklist() {
        editor2.remove("TRACK_LIST_RADIO");
        editor2.remove("TRACK_LIST_MIX");
        editor2.clear();
        editor2.commit();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGEDIN, false);
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }


    public void logoutUser() {
        editor.clear();
        editor.commit();
        UsersTable table = new UsersTable(context);
        table.open();
        table.deleteAll();
        table.close();
    }
}
