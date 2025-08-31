package com.example.kzmusic;

//Database imports
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


public class UsersFirestore {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;

    public UsersFirestore(Context context) {
        this.context = context;
    }

    //This function creates or updates a user document
    public void createOrUpdateUserDocument(String username, String email, String uid) {
        if (uid == null || uid.isEmpty()) {
            Log.e("Firebase", "Cannot create document for an invalid UID.");
            return;
        }

        // Use the UID as the unique document ID
        Map<String, Object> userData = new HashMap<>();
        userData.put("USERNAME", username);
        userData.put("EMAIL", email);
        userData.put("UID", uid); // It's still good practice to store the UID inside
        db.collection("Users").document(uid).set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "User document successfully written for UID: " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error writing user document for UID: " + uid, e);
                });
    }

    //This function updates the document in users collection
    public void updateUserDocument(String uid, String username, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("USERNAME", username);
        user.put("EMAIL", email);

        // Update the document that is keyed by the user's actual UID
        db.collection("Users").document(uid).update(user)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Data updated for UID: " + uid))
                .addOnFailureListener(e -> Log.e("Firebase", "Data update failed for UID: " + uid, e));
    }
}



