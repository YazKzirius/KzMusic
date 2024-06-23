package com.example.kzmusic;
//Importing important modules
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//This class implements the main page of the application
//Manages music listening and audio entertainment by mood
public class MainPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
    }
}