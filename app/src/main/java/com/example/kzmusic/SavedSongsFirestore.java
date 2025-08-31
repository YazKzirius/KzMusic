package com.example.kzmusic;

import android.content.Context;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.gms.tasks.OnSuccessListener;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedSongsFirestore {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    Context context;

    public SavedSongsFirestore(Context context) {
        this.context = context;
    }

    private String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    private CollectionReference getSavedSongsCollection(String uid) {
        return db.collection("Users").document(uid).collection("SavedSongs");
    }

    private String sanitizeTitleForDocId(String title) {
        // Replace forward slashes, which cause the crash, and other reserved characters.
        return title.replaceAll("[/\\\\*\\.\\[\\]~]", "-");
    }

    public void save_new_song(String email, String title, String album_url) {
        String uid = getCurrentUid();
        if (uid == null) {
            Log.e("FirebaseSecurity", "No authenticated user. Cannot save song.");
            return;
        }

        Map<String, Object> song = new HashMap<>();
        song.put("TITLE", title); // The original, full title is stored inside the document
        song.put("ALBUM_URL", album_url);

        // *** FIX: Sanitize the title for use as a document ID to prevent crashes ***
        String docId = sanitizeTitleForDocId(title);

        getSavedSongsCollection(uid).document(docId).set(song)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Song '" + title + "' saved."))
                .addOnFailureListener(e -> Log.e("Firebase", "Error saving song.", e));
    }

    public void remove_saved_song(String email, String title, String url) {
        String uid = getCurrentUid();
        if (uid == null) {
            Log.e("FirebaseSecurity", "No authenticated user. Cannot remove song.");
            return;
        }

        // *** FIX: Sanitize the title to find the correct document to delete ***
        String docId = sanitizeTitleForDocId(title);

        getSavedSongsCollection(uid).document(docId).delete()
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Song '" + title + "' removed."))
                .addOnFailureListener(e -> Log.e("Firebase", "Error removing song.", e));
    }

    public void is_saved(String email, String title, OnSuccessListener<Boolean> callback) {
        String uid = getCurrentUid();
        if (uid == null) {
            Log.e("FirebaseSecurity", "No authenticated user. Cannot check song status.");
            callback.onSuccess(false);
            return;
        }

        // *** FIX: Sanitize the title to check for the correct document's existence ***
        String docId = sanitizeTitleForDocId(title);

        getSavedSongsCollection(uid).document(docId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    callback.onSuccess(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error checking saved song.", e);
                    callback.onSuccess(false);
                });
    }
}