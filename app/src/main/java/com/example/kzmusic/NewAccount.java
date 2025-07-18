package com.example.kzmusic;
//Importing important modules
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
//Google  authorisation API
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

//This class implements the registration page of applications
//Allows the user to create a new account and adds to SQL Users table
public class NewAccount extends AppCompatActivity {
    //Interface attributes
    private static final int RC_SIGN_IN = 9001;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    String Username;
    String Email;
    String Password;
    Boolean is_registered = false;
    SessionManager sessionManager;
    String web_client_id = "251450547660-lprr10a6rtrlj97h3v6gn3h7jmskcgbj.apps.googleusercontent.com";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sessionManager = new SessionManager(this);
        //Creating button functionality
        create_back_btn();
        create_image_btn();
        create_account();
        create_google_icon();
        set_up_g_signin();
    }
    //This function creates functionality for logo image
    //Moves back to homepage
    public void create_image_btn() {
        findViewById(R.id.imageView1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate_to_activity(MainActivity.class);
            }
        });
    }
    //This function creates functionality for Google icon
    public void create_google_icon() {
        findViewById(R.id.google_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }
    //This function creates functionality for the back btn
    //Moves back to homepage
    public void create_back_btn() {
        Button back = findViewById(R.id.Back_btn2);
        back.setOnClickListener(v -> navigate_to_activity(MainActivity.class));
    }
    //This function creates functionality for create account btn
    //Creates an account by checking if data is validate using password validation and checks if account already exists.
    public void create_account() {
        Button create = findViewById(R.id.create2);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Validating data user entered
                validate_data();
                if (is_registered == true) {
                    //Adding new user and registering new user
                    //Moves to spotify authorisation
                    UsersFirestore table = new UsersFirestore(getApplicationContext());
                    table.registerUser(get_username(), get_email(), get_password());
                    sessionManager.createLoginSession(Username, Email);
                    navigate_to_activity(GetStarted.class);
                } else {
                    ;
                }
            }
        });
    }
    //This function navigates to a new activity given parameters
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(NewAccount.this, target);
        startActivity(intent);
    }
    //These functions get the data entered in the text boxes
    public String get_username() {
        EditText text = findViewById(R.id.User);
        return text.getText().toString();
    }
    public String get_email() {
        EditText text = findViewById(R.id.Email);
        return text.getText().toString();
    }
    public String get_password() {
        EditText text = findViewById(R.id.pass1);
        return text.getText().toString();
    }
    public String get_password2() {
        EditText text = findViewById(R.id.pass2);
        return text.getText().toString();
    }
    //This function validates user input and checks if ready to register
    public void validate_data() {
        //Getting user data
        String username = get_username();
        String email = get_email();
        String pass1 = get_password();
        String pass2 = get_password2();
        //Checking if email and username are valid
        if (email.contains("@") == false) {
            is_registered = false;
            Toast.makeText(this, "Registration Error: Email invalid", Toast.LENGTH_SHORT).show();
        } else if (username.equals("") == true) {
            is_registered = false;
            Toast.makeText(this, "Registration Error: Username invalid", Toast.LENGTH_SHORT).show();
        }
        //Password validation
        String message = validate_password(pass1);
        if (message.equals("Valid") == true) {
            if (pass1.equals(pass2) == false) {
                is_registered = false;
                //Error message
                Toast.makeText(this, "Registration Error: Passwords need to match", Toast.LENGTH_SHORT).show();
            } else {
                //Data is ok and ready for creating
                is_registered = true;
                Username = username;
                Email = email;
                Password = pass1;
            }
        } else {
            is_registered = false;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    //This function validates user password
    public String validate_password(String password) {
        //Checks if string has special characters, capital letter and number
        boolean hasSpecial = password.matches(".*[@!£$%^&*()/.,;:{}#~><=+?]+.*");
        boolean hasCapital = password.matches(".*[A-Z]+.*");
        boolean hasNumber = password.matches(".*[0-9]+.*");
        //Checks if string has more than 8 characters
        if (password.length() < 8) {
            is_registered = false;
            return "Registration Error: Password must have at least 8 characters";
        } else if (!hasSpecial) {
            is_registered = false;
            return "Registration Error: Password must have a special character";
        } else if (!hasCapital) {
            is_registered = false;
            return "Registration Error: Password must have a capital letter";
        } else if (!hasNumber) {
            is_registered = false;
            return "Registration Error: Password must have a number";
        } else {
            return "Valid";
        }
    }
    //This function manages google sign-in
    //Uses Google-API to sign-user into google account
    public void set_up_g_signin() {
        gso =  new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(web_client_id) // 🔥 Ensures Firebase retrieves the token
                .requestEmail()
                .build();

        gsc = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.Gsignin_btn2).setOnClickListener(new View.OnClickListener() {
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
            //Move to next activity
            Username = account.getDisplayName();
            Email = account.getEmail();
            authenticateWithFirebase(account);
            sessionManager.createLoginSession(Username, Email);
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