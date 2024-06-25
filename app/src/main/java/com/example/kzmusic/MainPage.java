package com.example.kzmusic;
//Importing important modules
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

//This class implements the main page of the application
//Manages music listening and audio entertainment by mood
public class MainPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_page);
        UsersTable table = new UsersTable(getApplicationContext());
        table.open();
        Cursor cursor = table.fetchAllAccounts();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String ID = cursor.getString(cursor.getColumnIndex("UserID"));
                String username = cursor.getString(cursor.getColumnIndex("USERNAME"));
                String email = cursor.getString(cursor.getColumnIndex("EMAIL"));
                Toast.makeText(this, "UserID: "+ID+", User: " + username + ", Email: " + email, Toast.LENGTH_SHORT).show();
            } while (cursor.moveToNext());
        }
        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        }
        //Fragment navigation menu
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment;
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
}