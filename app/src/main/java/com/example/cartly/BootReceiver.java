package com.example.cartly;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// AlarmManager forgets every alarm when the phone restarts, so the reminders are set again from the DB.
// Kept apart from NotificationReceiver so that one can stay non-exported: other apps can't make it post notifications.
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
            NotificationReceiver.scheduleAll(context);
    }
}
