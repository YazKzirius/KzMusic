package com.example.kzmusic;

//Importing important modules
import android.accounts.Account;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;

import android.widget.Toast;

//This class implements the Sign-in page for application
public class SignIn extends AppCompatActivity {
    //Interface attributes
    private static final int RC_SIGN_IN = 9001;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    String email;
    String password;
    SessionManager sessionManager;
    String web_client_id = "251450547660-lprr10a6rtrlj97h3v6gn3h7jmskcgbj.apps.googleusercontent.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sessionManager = new SessionManager(this);
        //Creating button functionality
        create_back_btn();
        set_up_signin();
        create_image_btn();
        create_google_icon();
        set_up_g_signin();
    }
    //This function creates functionality for logo image
    //Moves back to homepage if clicked
    public void create_image_btn() {
        findViewById(R.id.imageView2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate_to_activity(MainActivity.class);
            }
        });
    }
    //This function creates functionality for Google icon
    public void create_google_icon() {
        findViewById(R.id.google_icon2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }
    //This function creates functionality for the back btn
    //Moves back to homepage if clicked
    public void create_back_btn() {
        Button back = findViewById(R.id.Back_btn);
        back.setOnClickListener(v -> navigate_to_activity(MainActivity.class));
    }
    //This function gets email entered
    public String get_email() {
        EditText text = findViewById(R.id.TextEmailAddress);
        return text.getText().toString();
    }
    //This function gets password entered
    public String get_password() {
        EditText text = findViewById(R.id.TextPassword);
        return text.getText().toString();
    }
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(SignIn.this, target);
        startActivity(intent);
    }
    //This function checks if user details are valid in SQL Users table
    public void check_user_details() {
        //Getting entered data
        email = get_email();
        password = get_password();
        //Opening Users table
        UsersFirestore table = new UsersFirestore(getApplicationContext());
        //Signing in user
        table.db.collection("Users").whereEqualTo("EMAIL", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Sign-in Error: User doesn't exist", Toast.LENGTH_LONG).show();
                    } else {
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("FirebaseAuth", "User signed in successfully!");
                                        //Get specific username from email
                                        DocumentSnapshot document = querySnapshot.getDocuments().get(0); // Get first matching document
                                        String username = document.getString("USERNAME"); // Retrieve username
                                        sessionManager.createLoginSession(username, email);
                                        Toast.makeText(getApplicationContext(), "Welcome back: "+username.replaceAll(" ","")+"!", Toast.LENGTH_SHORT).show();
                                        navigate_to_activity(GetStarted.class);
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Sign-in Error: Invalid credentials entered", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });

    }
    //This function sets up sign in button
    //Implements checking functions
    public void set_up_signin() {
        findViewById(R.id.signin_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check_user_details();
            }
        });
    }
    //This function manages google sign-in
    //Uses Google-API to sign-user into google account
    public void set_up_g_signin() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(web_client_id) // 🔥 Ensures Firebase retrieves the token
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.Gsignin_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }
    //These following functions handle Google Sign-in functionality
    private void signIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            //Check if user exists already
            //Move to next activity
            authenticateWithFirebase(account);
            sessionManager.createLoginSession(account.getDisplayName(), account.getEmail());
            navigate_to_activity(GetStarted.class);
        //Throwing API exception and with error message
        } catch (ApiException e) {
            Toast.makeText(this, "Sign-in Error: Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }
    //This function handles Google sign-in with firebase
    private void authenticateWithFirebase(GoogleSignInAccount account) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (auth.getCurrentUser() != null) {
                            UsersFirestore table = new UsersFirestore(getApplicationContext());
                            table.add_account(account.getDisplayName(), account.getEmail(), auth.getCurrentUser().getUid());
                            Log.d("Firebase", "Registered with Google");
                        }
                    } else {
                        Log.e("FirebaseAuth", "Google sign-in failed", task.getException());
                    }
                });
    }


}