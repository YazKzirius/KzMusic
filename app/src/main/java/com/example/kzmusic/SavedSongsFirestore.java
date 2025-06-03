package com.example.kzmusic;

import android.content.Context;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedSongsFirestore {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Context context;
    public SavedSongsFirestore(Context context) {
        this.context = context;
    }
    //This function saves a new song to the Saved songs collection
    public void save_new_song(String email, String title, String album_url) {
        db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String user_id = querySnapshot.getDocuments().get(0).getId();
                        // 🔥 Check if the same song exists for the user
                        db.collection("SavedSongs")
                                .whereEqualTo("TITLE", title)
                                .whereEqualTo("USER_ID", user_id) // Ensure user does not have this song already
                                .get()
                                .addOnSuccessListener(songSnapshot -> {
                                    if (songSnapshot.isEmpty()) {
                                        // ✅ Song is unique for this user, proceed to add
                                        Map<String, Object> song = new HashMap<>();
                                        song.put("TITLE", title);
                                        song.put("ALBUM_URL", album_url);
                                        song.put("USER_ID", user_id);

                                        db.collection("SavedSongs")
                                                .add(song)
                                                .addOnSuccessListener(documentReference ->
                                                        Log.d("Firebase", "Song Saved with ID: " + documentReference.getId()))
                                                .addOnFailureListener(e ->
                                                        Log.e("Firebase", "Error saving song", e));
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
    //This function removes a saved song from the collection
    public void remove_saved_song(String email, String title) {
        db.collection("Users").whereEqualTo("EMAIL", email).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String user_id = querySnapshot.getDocuments().get(0).getId();

                        // 🔥 Find the saved song for this user
                        db.collection("SavedSongs")
                                .whereEqualTo("TITLE", title)
                                .whereEqualTo("USER_ID", user_id)
                                .get()
                                .addOnSuccessListener(songSnapshot -> {
                                    if (!songSnapshot.isEmpty()) {
                                        // ✅ Delete the first matching song document
                                        String songId = songSnapshot.getDocuments().get(0).getId();
                                        db.collection("SavedSongs").document(songId)
                                                .delete()
                                                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Song removed successfully!"))
                                                .addOnFailureListener(e -> Log.e("Firebase", "Error removing song", e));
                                    } else {
                                        Log.e("Firebase", "No saved song found for this user.");
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("Firebase", "Error retrieving saved song", e));
                    } else {
                        Log.e("Firebase", "User not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error retrieving user", e));
    }
    //This function checks if a song is saved in firestore collection
    public boolean is_saved(String email, String title) {
        try {
            // 🔍 Get user ID first
            QuerySnapshot userSnapshot = Tasks.await(db.collection("Users")
                    .whereEqualTo("EMAIL", email)
                    .limit(1)
                    .get());
            Toast.makeText(context, "user", Toast.LENGTH_LONG).show();
            if (userSnapshot.isEmpty()) {
                return false; // User not found
            }

            String userId = userSnapshot.getDocuments().get(0).getId();

            // 🔥 Check if song exists in liked songs
            QuerySnapshot songSnapshot = Tasks.await(db.collection("SavedSongs")
                    .whereEqualTo("USER_ID", userId)
                    .whereEqualTo("TITLE", title)
                    .get());

            return !songSnapshot.isEmpty(); // ✅ Returns true if song is liked, false otherwise
        } catch (Exception e) {
            Log.e("Firestore", "Error checking liked song", e);
            return false; // 🔥 Default to false on failure
        }
    }

}
