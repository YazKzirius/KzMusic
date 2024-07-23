package com.example.kzmusic;

//Imports
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//This class broadcasts the live alarm
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Start the activity
        Intent activityIntent = new Intent(context, GetStarted.class);
        context.startActivity(activityIntent);
    }
}