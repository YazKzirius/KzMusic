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
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
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

        if (savedInstanceState == null) { // <--- ADD THIS CHECK
            Toast.makeText(this, "Welcome " + username + "!", Toast.LENGTH_LONG).show();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            Intent serviceIntent = new Intent(this, TokenRefreshService.class);
            startService(serviceIntent);
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
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (OfflinePlayerManager.getInstance().current_player != null) {
          OfflinePlayerManager.getInstance().current_player.pause();
        }
    }
}
