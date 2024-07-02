package com.example.kzmusic;

//Importing important modules
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
        Boolean is_valid = false;
        //Getting entered data
        email = get_email();
        password = get_password();
        //Opening Users table
        UsersTable table = new UsersTable(getApplicationContext());
        table.open();
        //Checking if user exists
        if (table.user_exists(email)) {
            //Checking if user exists
            is_valid = table.checkLogin(email, password);
            if (is_valid) {
                //Navigating to new activity with display message
                Toast.makeText(getApplicationContext(), "Welcome back: "+table.find_name_by_email(email).replaceAll(" ","")+"!", Toast.LENGTH_SHORT).show();
                sessionManager.createLoginSession(table.find_name_by_email(email), email);
                navigate_to_activity(GetStarted.class);
                table.close();
            } else {
                //Displaying error message
                Toast.makeText(getApplicationContext(), "Sign-in Error: Invalid Credentials entered", Toast.LENGTH_SHORT).show();
            }
        } else {
            //Displaying another error message
            Toast.makeText(getApplicationContext(), "Sign-in Error: User doesn't exist", Toast.LENGTH_SHORT).show();
        }
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
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
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
            //If exists continue as usual, if not sign in and add to table, else continue as usual
            UsersTable table = new UsersTable(getApplicationContext());
            table.open();
            if (!table.user_exists(account.getEmail())) {
                table.add_account(account.getDisplayName(), account.getEmail(), "");
                sessionManager.createLoginSession(account.getDisplayName(), account.getEmail());
                navigate_to_activity(GetStarted.class);
            } else {
                sessionManager.createLoginSession(account.getDisplayName(), account.getEmail());
                navigate_to_activity(GetStarted.class);
            }
        //Throwing API exception and with error message
        } catch (ApiException e) {
            Toast.makeText(this, "Sign-in Error: Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }


}