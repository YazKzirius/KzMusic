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

//This class implements the application homepage
//Allows users to login or create account
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //Creating button functionality
        set_up_login();
        set_up_create();
    }
    //This function sets up the login button
    //Moves to Sign-in page
    public void set_up_login() {
        Button get_started = findViewById(R.id.Login_btn);
        get_started.setOnClickListener(v -> navigate_to_activity(SignIn.class));
    }
    //This function sets up the Create account button
    //Moves to New account page
    public void set_up_create() {
        Button get_started = findViewById(R.id.create);
        get_started.setOnClickListener(v -> navigate_to_activity(NewAccount.class));
    }

    //This function navigates to the given activity in parameter
    public void navigate_to_activity(Class <?> target) {
        Intent intent = new Intent(MainActivity.this, target);
        startActivity(intent);
    }
}