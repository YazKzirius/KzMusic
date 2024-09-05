package com.example.kzmusic;
//Important modules
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

//This class implements the methods needed for the Users table of database
//Storing ID, username, email and password
public class UsersTable {
    private KzmusicDatabase main_db;
    private SQLiteDatabase db;

    public UsersTable(Context context) {
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
    //This function fetches all the data in Users table
    public Cursor fetchAllAccounts() {
        String[] columns = {
                "UserID",
                "USERNAME",
                "EMAIL",
                "PASSWORD"
        };
        Cursor cursor = db.query("Users", columns, null, null, null, null, null);
        return cursor;
    }
    //This function updates account information by userID
    public int updateAccount(long id, String username, String email, String password) {
        ContentValues values = new ContentValues();
        values.put("USERNAME", username);
        values.put("EMAIL", email);
        values.put("PASSWORD", password);
        return db.update("Users", values, "UserID" + " = " + id, null);
    }
    //This function checks if account login details by email are valid
    public boolean checkLogin(String email, String password) {
        String[] columns = { "UserID" };
        String selection = "EMAIL" + " = ? AND " + "PASSWORD" + " = ?";
        String[] selectionArgs = { email, password };

        Cursor cursor = db.query(
                "Users",
                columns,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }
    //This checks if a user exists in Users table by email
    public Boolean user_exists(String email) {
        email = email.toLowerCase();
        String[] columns = {"UserID"};
        String selection = "EMAIL = ?";
        String[] args = {email};
        Cursor cursor = db.query("Users", columns, selection, args, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    //This function finds account usernames by email
    public String find_name_by_email(String email) {
        String[] columns = {"USERNAME"};
        String selection = "EMAIL = ?";
        String[] args = {email};
        String username = "";
        Cursor cursor = db.query("Users", columns, selection, args, null, null, null);
        if (cursor != null && cursor.moveToNext()) {
            username = cursor.getString(cursor.getColumnIndex("USERNAME"));
        }
        cursor.close();
        return username;
    }
    //This function finds account usernames by email
    public int find_id_by_email(String email) {
        String[] columns = {"UserID"};
        String selection = "EMAIL = ?";
        String[] args = {email};
        int id = -1;
        Cursor cursor = db.query("Users", columns, selection, args, null, null, null);
        if (cursor != null && cursor.moveToNext()) {
             id = cursor.getInt(cursor.getColumnIndex("UserID"));
        }
        cursor.close();
        return id;
    }
    //This function adds new account to Users table
    public long add_liked_song(String email, String title, String url) {
        ContentValues values = new ContentValues();
        values.put("UserID", find_id_by_email(email));
        values.put("TITLE", title);
        values.put("ALBUM_URL", url);
        values.put("TIMES_PLAYED", 0);
        return db.insert("LikedSongs", null, values);
    }
    //This function checks if song is in liked songs
    public Boolean song_liked(String title, String email) {
        String[] columns = {"likedSongID"};
        String selection = "TITLE = ?"+"AND UserID = "+find_id_by_email(email);
        String[] args = {title};
        Cursor cursor = db.query("LikedSongs", columns, selection, args, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;

    }
    //This function fetches all the data in Users table
    public Cursor fetchAllLiked(String email) {
        String[] columns = {
                "UserID",
                "likedSongID",
                "TITLE",
                "ALBUM_URL",
                "TIMES_PLAYED"
        };
        String selection = "UserID = "+find_id_by_email(email);
        Cursor cursor = db.query("LikedSongs", columns, selection, null, null, null, null);
        return cursor;
    }
    //This function removes a liked song
    public void remove_liked(String email, String name) {
        SQLiteDatabase db = main_db.getWritableDatabase();
        // Find the user ID based on the email
        int userId = find_id_by_email(email);

        // Define the whereClause with placeholders
        String whereClause = "UserID = ? AND TITLE = ?";
        // Define the whereArgs with actual values
        String[] whereArgs = new String[]{String.valueOf(userId), name};

        // Execute the delete operation
        db.delete("LikedSongs", whereClause, whereArgs);
    }
    //This function gets album url by ID and name
    public String get_album_url(String email, String name) {
        String[] columns = {"ALBUM_URL"};
        String selection = "UserID = "+find_id_by_email(email) + " AND TITLE = ?";
        String[] args = {name};
        Cursor cursor = db.query("LikedSongs", columns, selection, args, null, null, null);
        String url = "";
        if (cursor != null && cursor.moveToNext()) {
            url = cursor.getString(cursor.getColumnIndex("ALBUM_URl"));
        }
        cursor.close();
        return url;
    }

    //This function deletes an account by ID
    public void deleteAccount(long id) {
        db.delete("Users", "UserID" + "=" + id, null);
    }

    //This function deletes all accounts
    public void deleteAll() {
        Cursor cursor = fetchAllAccounts();
        while (cursor.moveToNext()) {
            String ID2 = cursor.getString(cursor.getColumnIndex("UserID"));
            deleteAccount(Long.valueOf(ID2));
        }
    }
}
