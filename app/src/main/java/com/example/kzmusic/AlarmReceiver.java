package com.example.kzmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Start the activity
        Intent activityIntent = new Intent(context, GetStarted.class);
        context.startActivity(activityIntent);
    }
}