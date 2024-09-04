package com.example.kzmusic;

//Imports
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
public class LikedTable {
    private KzmusicDatabase main_db;
    private SQLiteDatabase db;

    public LikedTable(Context context) {
        main_db = new KzmusicDatabase(context);
    }
    //This function opens the database
    public void open() {
        db = main_db.getWritableDatabase();
    }
    //This function closes the database
    public void close() {
        main_db.close();
    }
    //This function adds new account to Users table
    public long add_account(String username, String email, String password) {
        ContentValues values = new ContentValues();
        values.put("USERNAME", username);
        values.put("EMAIL", email);
        values.put("PASSWORD", password);
        return db.insert("Users", null, values);
    }
}
