package com.example.kzmusic;
//Importing important modules
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.app.AlarmManager;
import android.app.PendingIntent;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//This class implements the main page of the application
//Manages music listening and audio entertainment by mood
public class MainPage extends AppCompatActivity {
    Fragment fragment;
    String email;
    String username;
    SessionManager sessionManager;
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String CLIENT_SECRET = "7c15410b4f714a839cc3ad8f661a6513";
    String REDIRECT_URI = "kzmusic://callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    int REQUEST_CODE = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_page);
        Bundle bundle = getIntent().getExtras();
        sessionManager = new SessionManager(getApplicationContext());
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        Toast.makeText(this, "Welcome " + username+"!", Toast.LENGTH_SHORT).show();
        if (bundle != null ) {
            String token = bundle.getString("Token");
            long exp_time = bundle.getLong("expiration_time");
            String refresh = bundle.getString("refresh");
            schedule_token_refresh(exp_time-120, refresh);
            send_data(token, exp_time);
        } else {
            ;
        }
        //Default fragment
        //Setting token refresh time 2 minutes before expiration
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        }
        create_fragments();
    }
    //This function sends user data to fragments
    public void send_data(String t, long time) {
        // Sending data to Home fragment
        Bundle bundle = new Bundle();
        bundle.putString("Token", t);
        bundle.putLong("expiration_time", time);
        Fragment homeFragment = new HomeFragment();
        homeFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .addToBackStack(null)
                .commit();

        // Sending data to Search fragment
        Fragment searchFragment = new SearchFragment();
        searchFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, searchFragment)
                .addToBackStack(null)
                .commit();

        // Sending data to Library fragment
        Fragment libraryFragment = new LibraryFragment();
        libraryFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, libraryFragment)
                .addToBackStack(null)
                .commit();
    }
    //This function creates main page fragments
    public void create_fragments() {
        //Fragment navigation menu
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_search) {
                fragment = new SearchFragment();
            } else if (id == R.id.nav_library) {
                fragment = new LibraryFragment();
            } else if (id == R.id.nav_account) {
                fragment = new AccountSettingsFragment();
            } else {
                return false;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
            return true;
        });
    }
    public void schedule_token_refresh(long refresh_time, String r) {
        long refreshTime = System.currentTimeMillis() + refresh_time * 1000;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshAccessToken(r);
                // Schedule the next refresh
                handler.postDelayed(this, refreshTime); // 55 minutes
            }
        };
        // Start the initial refresh
        handler.post(refreshRunnable);
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
    //This function updates the total song duration attribute in databse
    public void update_total_duration() {
        long duration = OfflinePlayerManager.getInstance().current_player.getCurrentPosition() - SongQueue.getInstance().last_postion;
        String display_title = format_title(SongQueue.getInstance().current_song.getName()) + " by " + SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", ");
        //Updating song duration database
        SessionManager sessionManager = new SessionManager(getApplicationContext());
        String email = sessionManager.getEmail();
        UsersTable table = new UsersTable(getApplicationContext());
        table.open();
        table.update_song_duration(email, display_title, (int) (duration/(1000 * SongQueue.getInstance().speed)));
        table.close();
    }
    // This function refreshes an expired access token
    public void refreshAccessToken(String refresh) {
        if (refresh != null) {
            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refresh)
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .build();

            Request request = new Request.Builder()
                    .url(TOKEN_URL)
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainPage.this, "Failed to refresh token", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String newAccessToken = json.getString("access_token");
                            long newExpirationTime = json.getLong("expires_in");

                            // Ensure UI updates are made on the main thread
                            runOnUiThread(() -> {
                                OnlinePlayerManager.getInstance().setAccess_token(newAccessToken);
                                OnlinePlayerManager.getInstance().setExpiration_time(newExpirationTime);
                                // Schedule the next token refresh
                                schedule_token_refresh(newExpirationTime - 120, refresh);
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(MainPage.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainPage.this, "Failed to refresh token: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } else {
            Toast.makeText(MainPage.this, "Refresh token is null", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        OnlinePlayerManager.getInstance().stopPlaybackAndDisconnect();
        OfflinePlayerManager.getInstance().stopAllPlayers();
        //Stopping all notification sessions for single session management
        if (OfflinePlayerManager.getInstance().get_size() > 0) {
            OfflinePlayerManager.getInstance().StopAllSessions();
        }
        update_total_duration();
        SongQueue.getInstance().clear_songs();
    }
}
