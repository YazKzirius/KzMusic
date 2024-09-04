package com.example.kzmusic;
//Importing important modules
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;

//This class implements the main page of the application
//Manages music listening and audio entertainment by mood
public class MainPage extends AppCompatActivity {
    Fragment fragment;
    String email;
    String username;
    String token;
    long expiration_time;
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
        if (bundle != null) {
            token = bundle.getString("Token");
            expiration_time = bundle.getLong("expiration_time");
            schedule_token_refresh(expiration_time-120);
            send_data();
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
    public void send_data() {
        // Sending data to Home fragment
        Bundle bundle = new Bundle();
        bundle.putString("Token", token);
        bundle.putLong("expiration_time", expiration_time);
        fragment = new HomeFragment();
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        //Sending it to Search fragment
        Bundle bundle2 = new Bundle();
        bundle2.putString("Token", token);
        bundle2.putLong("expiration_time", expiration_time);
        fragment = new SearchFragment();
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        //Sending it to Search fragment
        Bundle bundle3 = new Bundle();
        bundle3.putString("Token", token);
        bundle3.putLong("expiration_time", expiration_time);
        fragment = new LibraryFragment();
        fragment.setArguments(bundle3);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
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
    public void schedule_token_refresh(long refresh_time) {
        long refreshTime = System.currentTimeMillis() + refresh_time * 1000;
        Intent intent = new Intent(this, AlarmReceiver.class);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, refreshTime, pendingIntent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        SpotifyPlayerLife.getInstance().stopPlaybackAndDisconnect();
        PlayerManager.getInstance().stopAllPlayers();
        //Stopping all notification sessions for single session management
        if (PlayerManager.getInstance().get_size() > 0) {
            PlayerManager.getInstance().StopAllSessions();
        }
        SongQueue.getInstance().clear_songs();
    }
}
