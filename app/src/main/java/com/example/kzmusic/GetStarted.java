package com.example.kzmusic;
//Imports
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class GetStarted extends AppCompatActivity {
    String CLIENT_ID = "21dc131ad4524c6aae75a9d0256b1b70";
    String REDIRECT_URI = "kzmusic://callback";
    int REQUEST_CODE = 1337;
    String username;
    String email;
    String token;
    long expiration_time;
    SessionManager sessionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_get_started);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(GetStarted.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            findViewById(R.id.Get_started_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    set_up_spotify();
                }
            });
        }
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
                    token = response.getAccessToken();
                    expiration_time = response.getExpiresIn();
                    //Sending email data to next activity
                    Bundle bundle = new Bundle();
                    bundle.putString("Token", token);
                    bundle.putLong("expiration_time", expiration_time);
                    Intent new_intent = new Intent(GetStarted.this, MainPage.class);
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