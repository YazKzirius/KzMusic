package com.example.kzmusic;

//Database imports
import android.content.Context;
import android.widget.Toast;

import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.HashMap;
import java.util.Map;

public class KzmusicFirebase {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Context context;

    public KzmusicFirebase(Context context) {
        this.context = context;
    }

    //This function creates new Users table in google firebase
    public void create_users() {
        Map<String, Object> sampleUser = new HashMap<>();
        sampleUser.put("USERNAME", "DemoUser");
        sampleUser.put("EMAIL", "demo@example.com");
        sampleUser.put("PASSWORD", "hashedpassword");
        db.collection("Users").document("demo@example.com")
                .set(sampleUser)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Users collection created", Toast.LENGTH_LONG))
                .addOnFailureListener(e -> Toast.makeText(context, "Users creation error", Toast.LENGTH_LONG));
    }
    //This function creates new songs table in google firebase
    public void createSongsCollection() {
        Map<String, Object> sampleSong = new HashMap<>();
        sampleSong.put("UserID", "demo@example.com");
        sampleSong.put("TITLE", "Demo Song");
        sampleSong.put("TOTAL_DURATION", 180);
        sampleSong.put("TIMES_PLAYED", 5);

        db.collection("Songs").add(sampleSong)
                .addOnSuccessListener(documentReference -> Toast.makeText(context, "Songs creation success", Toast.LENGTH_LONG))
                .addOnFailureListener(e -> Toast.makeText(context, "Songs creation error", Toast.LENGTH_LONG));
    }
    //This function adds a new user to table
    public void addUser(String userId, String username, String email, String passwordHash) {
        Map<String, Object> user = new HashMap<>();
        user.put("USERNAME", username);
        user.put("EMAIL", email);
        user.put("PASSWORD_HASH", passwordHash);

        db.collection("Users").document(userId)
                .set(user, SetOptions.merge())  // Prevents overwriting existing user data
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "User added successfully", Toast.LENGTH_LONG))
                .addOnFailureListener(e -> Toast.makeText(context, "Error adding user", Toast.LENGTH_LONG));
    }
    //This function adds a new Song to table
    public void addSong(String userId, String songTitle, int duration, int timesPlayed) {
        Map<String, Object> song = new HashMap<>();
        song.put("UserID", userId);
        song.put("TITLE", songTitle);
        song.put("TOTAL_DURATION", duration);
        song.put("TIMES_PLAYED", timesPlayed);

        db.collection("Songs").add(song)
                .addOnSuccessListener(documentReference -> Toast.makeText(context, "Song added", Toast.LENGTH_LONG))
                .addOnFailureListener(e -> Toast.makeText(context, "Error adding song", Toast.LENGTH_LONG));
    }




}
