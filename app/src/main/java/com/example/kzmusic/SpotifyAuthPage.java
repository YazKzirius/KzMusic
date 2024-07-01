package com.example.kzmusic;

import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.spotify.sdk.android.auth.AuthorizationClient;
import androidx.fragment.app.Fragment;

public class SpotifyAuthPage extends AppCompatActivity {
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String REDIRECT_URI = "kzmusic://callback";
    int REQUEST_CODE = 1337;
    String username;
    String email;
    String password;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_spotify_auth_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        set_up_spotify();
    }
    //These functions sets up the Spotify Sign-in/authorisation using spotify API
    public void set_up_spotify() {
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    //Display message
                    Toast.makeText(this, "Spotify Authorisation success", Toast.LENGTH_SHORT).show();
                    //Adding account to SQL database
                    UsersTable table = new UsersTable(getApplicationContext());
                    table.open();
                    token = response.getAccessToken();
                    Bundle data = getIntent().getExtras();
                    username = data.getString("Username");
                    email = data.getString("Email");
                    password = data.getString("Password");
                    long id = table.add_account(username+", "+token, email, password);
                    table.close();
                    //Sending email data to next activity
                    Bundle bundle = new Bundle();
                    bundle.putString("email", email);
                    Toast.makeText(this, "Welcome " + username+"!", Toast.LENGTH_SHORT).show();
                    Intent new_intent = new Intent(SpotifyAuthPage.this, MainPage.class);
                    new_intent.putExtras(bundle);
                    startActivity(new_intent);
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Toast.makeText(this, "Spotify Authorisation error", Toast.LENGTH_SHORT).show();
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }
}