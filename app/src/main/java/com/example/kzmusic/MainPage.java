package com.example.kzmusic;
//Importing important modules
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
        sessionManager.setCurrent_activity(MainPage.class);
        if (savedInstanceState == null) { // <--- ADD THIS CHECK
            Toast.makeText(this, "Welcome " + username + "!", Toast.LENGTH_LONG).show();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            Intent serviceIntent = new Intent(getApplicationContext(), TokenRefreshService.class);
            startService(serviceIntent); // immediate call works, because intent is here and ready
        }
        boolean openOverlay = getIntent().getBooleanExtra("openMediaOverlay", false);
        if (openOverlay) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MediaOverlay())
                    .addToBackStack(null)
                    .commit();

            // Clear the flag to prevent re-triggering on future resumes
            getIntent().removeExtra("openMediaOverlay");
        }
        // create_fragments() can usually stay outside this check,
        // as you'd want to set up your fragments regardless.
        create_fragments();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean openOverlay = intent.getBooleanExtra("openMediaOverlay", false);
        if (openOverlay) {
            if (SongQueue.getInstance().current_song != null) {
                SongQueue.getInstance().addSong(SongQueue.getInstance().current_song);
                SongQueue.getInstance().setPosition(SongQueue.getInstance().current_position);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MediaOverlay())
                    .addToBackStack(null)
                    .commit();

            // Clear the flag to prevent re-triggering on future resumes
            intent.removeExtra("openMediaOverlay");
        }
    }
    //This function schedules access token refresh by expiration time
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
            } else if (id == R.id.nav_genAI) {
                fragment = new GenAIFragment();
            } else if (id == R.id.nav_library) {
                fragment = new LibraryFragment();
            } else if (id == R.id.nav_account) {
                fragment = new AccountSettingsFragment();
            }
            else {
                return false;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
            return true;
        });
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (OfflinePlayerManager.getInstance().current_player != null) {
          OfflinePlayerManager.getInstance().current_player.pause();
        }
    }
}
