package com.example.kzmusic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.database.Cursor;

public class NewAccount extends AppCompatActivity {
    String Username;
    String Email;
    String Password;
    Boolean is_registered = false;
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
        create_back_btn();
        create_account();
    }
    //This function creates functionality for the back btn
    public void create_back_btn() {
        Button back = findViewById(R.id.Back_btn2);
        back.setOnClickListener(v -> navigate_to_activity(MainActivity.class));
    }
    //This function creates functionality for create account btn
    public void create_account() {
        Button create = findViewById(R.id.create2);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validate_data();
                if (is_registered == true) {
                    Toast.makeText(getApplicationContext(), "Registration successful", Toast.LENGTH_SHORT).show();
                    UsersTable table = new UsersTable(getApplicationContext());
                    table.open();
                    add_data();
                    table.close();
                    navigate_to_activity(MainPage.class);
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
        String username = get_username();
        String email = get_email();
        String pass1 = get_password();
        String pass2 = get_password2();
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
                Toast.makeText(this, "Registration Error: Passwords need to match", Toast.LENGTH_SHORT).show();
            } else {
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
        boolean hasSpecial = password.matches(".*[@!£$%^&*()/.,;:{}#~><=+?]+.*");
        boolean hasCapital = password.matches(".*[A-Z]+.*");
        boolean hasNumber = password.matches(".*[0-9]+.*");
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

    //This function adds data to database
    public void add_data() {
        UsersTable table = new UsersTable(getApplicationContext());
        table.open();
        long id = table.add_account(Username, Email, Password);
        table.close();
    }



}