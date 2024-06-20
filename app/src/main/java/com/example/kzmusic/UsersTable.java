package com.example.kzmusic;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UsersTable {
    private KzmusicDatabase main_db;
    private SQLiteDatabase db;

    public UsersTable(Context context) {
        main_db = new KzmusicDatabase(context);
    }
    public void open() {
        db = main_db.getWritableDatabase();
    }
    public void close() {
        main_db.close();
    }
    //This function adds account to Users table
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
    //This function updates account information
    public int updateAccount(long id, String username, String email, String password) {
        ContentValues values = new ContentValues();
        values.put("USERNAME", username);
        values.put("EMAIL", email);
        values.put("PASSWORD", password);
        return db.update("Users", values, "UserID" + " = " + id, null);
    }
    //This function deletes an account
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
