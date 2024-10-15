package com.example.kzmusic;
//Importing important modules
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.app.AlarmManager;
import android.app.PendingIntent;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        //Default fragment
        //Setting token refresh time 2 minutes before expiration
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        }
        create_fragments();
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
