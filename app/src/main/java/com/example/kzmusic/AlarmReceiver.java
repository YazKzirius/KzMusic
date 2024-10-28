package com.example.kzmusic;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent newIntent = new Intent(context, GetStarted.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newIntent);
    }
}






