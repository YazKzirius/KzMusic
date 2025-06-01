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
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    SessionManager sessionManager;

    Context context;
    public UsersFirestore(Context context) {
        this.context = context;
    }

    //This function adds a new user and creates new table in google firestore
    public void add_account(String username, String email, String UID) {
        db.collection("Users").whereEqualTo("EMAIL", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Map<String, Object> sampleUser = new HashMap<>();
                        sampleUser.put("USERNAME", username);
                        sampleUser.put("EMAIL", email);
                        sampleUser.put("UID", UID);
                        db.collection("Users")
                                .add(sampleUser)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d("Firebase", "User created with ID: " + documentReference.getId());
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
    //This function registers a new user using Firebase auth
    public void registerUser(String username, String email, String password) {
        db.collection("Users").whereEqualTo("EMAIL", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        add_account(username, email, auth.getCurrentUser().getUid());
                                        Toast.makeText(context, "Registered", Toast.LENGTH_LONG).show();
                                        Log.d("FirebaseAuth", "User registered successfully!");
                                    } else {
                                        Log.e("FirebaseAuth", "Error registering user", task.getException());
                                    }
                                });
                    } else {
                        Toast.makeText(context, "Registration Error: This account is already registered, please sign-in.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    //This function updates user information in user collection
    public void update_user(String ID, String username, String email) {
        //Storing data in hash-map
        Map<String, Object> user = new HashMap<>();
        user.put("USERNAME", username);
        user.put("EMAIL", email);
        user.put("UID", FirebaseAuth.getInstance().getCurrentUser().getUid());
        db.collection("Users").document(ID)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Firebase", "Data updates at "+ID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Firebase", "Data update failed for "+ID);
                    }
                });
    }






}
