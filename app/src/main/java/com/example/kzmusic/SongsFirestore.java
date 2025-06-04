package com.example.kzmusic;

//Imports
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SongsFirestore {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Context context;
    public SongsFirestore(Context context) {
        this.context = context;
    }
    //This function adds a new song to Songs collection
    public void add_new_song(String email, String title, String artist) {
        db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String user_id = querySnapshot.getDocuments().get(0).getId();

                        // 🔥 Check if the same song exists for the user
                        db.collection("Songs")
                                .whereEqualTo("TITLE", title)
                                .whereEqualTo("USER_ID", user_id) // Ensure user does not have this song already
                                .get()
                                .addOnSuccessListener(songSnapshot -> {
                                    if (songSnapshot.isEmpty()) {
                                        // ✅ Song is unique for this user, proceed to add
                                        Map<String, Object> song = new HashMap<>();
                                        song.put("TITLE", title);
                                        song.put("ARTIST", artist);
                                        song.put("TOTAL_DURATION", 0);
                                        song.put("TIMES_PLAYED", 1);
                                        song.put("USER_ID", user_id);

                                        db.collection("Songs")
                                                .add(song)
                                                .addOnSuccessListener(documentReference ->
                                                        Log.d("Firebase", "Song added with ID: " + documentReference.getId()))
                                                .addOnFailureListener(e ->
                                                        Log.e("Firebase", "Error adding song", e));
                                    } else {
                                        Log.e("Firebase", "Duplicate song detected for user.");
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("Firebase", "Error checking song existence", e));
                    } else {
                        Log.e("Firebase", "User not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error retrieving user", e));
    }
    //This function updates the number of times played in Songs collection
    public void updateTimesPlayed(String email, String title) {
        db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!userSnapshot.isEmpty()) {
                        String userId = userSnapshot.getDocuments().get(0).getId();

                        // 🔍 Find the song belonging to this user
                        db.collection("Songs").whereEqualTo("TITLE", title).whereEqualTo("USER_ID", userId).get()
                                .addOnSuccessListener(songSnapshot -> {
                                    if (!songSnapshot.isEmpty()) {
                                        String songId = songSnapshot.getDocuments().get(0).getId();

                                        // ✅ Increase times played
                                        db.collection("Songs").document(songId)
                                                .update("TIMES_PLAYED", FieldValue.increment(1))
                                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Times played updated!"))
                                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating times played", e));
                                    } else {
                                        Log.e("Firestore", "Song not found for this user.");
                                    }
                                });
                    } else {
                        Log.e("Firestore", "User not found.");
                    }
                });
    }
    //This function gets the top 10 most played songs
    public void get_top_played_songs(String email, OnSuccessListener<List<String>> callback) {
        db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!userSnapshot.isEmpty()) {
                        String userId = userSnapshot.getDocuments().get(0).getId();

                        db.collection("Songs")
                                .whereEqualTo("USER_ID", userId) // 🔥 Filter by specific user
                                .orderBy("TIMES_PLAYED",  Query.Direction.DESCENDING)
                                .get()
                                .addOnSuccessListener(songSnapshot -> {
                                    List<String> songNames = new ArrayList<>();
                                    for (DocumentSnapshot document : songSnapshot.getDocuments()) {
                                        songNames.add(document.getString("TITLE"));
                                    }
                                    callback.onSuccess(songNames);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Error retrieving top songs", e);
                                    callback.onSuccess(Collections.emptyList());
                                });
                    } else {
                        callback.onSuccess(Collections.emptyList());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error retrieving user", e);
                    callback.onSuccess(Collections.emptyList());
                });
    }

}
