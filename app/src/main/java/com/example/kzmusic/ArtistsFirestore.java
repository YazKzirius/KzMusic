package com.example.kzmusic;

//imports
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.HashMap;
import java.util.Map;
public class ArtistsFirestore {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    Context context;
    public ArtistsFirestore(Context context) {
        this.context = context;
    }
    //This function adds a new artist to the Artists collection
    public void add_new_artist(String email, String name) {
        db.collection("Users").whereEqualTo("EMAIL", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        ;
                    } else {
                        db.collection("Artists").whereEqualTo("NAME", name).get()
                                .addOnSuccessListener(querySnapshot1 -> {
                                    if (querySnapshot1.isEmpty()) {
                                        Map<String, Object> artist = new HashMap<>();
                                        artist.put("NAME", name);
                                        artist.put("UserID", querySnapshot.getDocuments().get(0).getId());
                                        db.collection("Artist")
                                                .add(artist)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {
                                                        Log.d("Firebase", "Artist added with ID: " + documentReference.getId());
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.d("Firebase", e.getMessage());
                                                    }
                                                });
                                    } else {
                                        ;
                                    }
                                });
                    }
                });
    }


}
