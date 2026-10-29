package com.example.cartly;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

// Shows the reminder an hour before a planned shopping.
public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "MyNotificationChannel";  // kept from v1, so the channel is renamed rather than duplicated
    private static final long ONE_HOUR = 60 * 60 * 1000;

    /** Sets the reminders of every upcoming shopping again: alarms don't survive a reboot or a "force stop". */
    static void scheduleAll(Context context) {
        for (DBManager.Shopping shopping : new DBManager(context).findUpcomingShoppings())
            schedule(context, shopping);
    }

    /** Sets the reminder one hour before a planned shopping; nothing if that moment is already past. */
    static void schedule(Context context, DBManager.Shopping shopping) {
        long at;
        try {
            at = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).parse(shopping.date + " " + shopping.time).getTime() - ONE_HOUR;
        } catch (ParseException e) {
            return;
        }
        if (at <= System.currentTimeMillis())
            return;

        Intent intent = new Intent(context, NotificationReceiver.class).putExtra("id", shopping.id);
        // One PendingIntent per shopping: extras don't make Intents distinct, so a shared
        // requestCode would make every new alarm replace the previous one.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, shopping.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // inexact, but unlike set() it still fires while the phone dozes; exact alarms need SCHEDULE_EXACT_ALARM
        context.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // read the shopping now: it may have been done, deleted or changed since the reminder was set
        DBManager.Shopping shopping = new DBManager(context).findShopping(intent.getIntExtra("id", -1));
        if (shopping == null || shopping.type != DBManager.PLANNED)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return;

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT));

        Intent open = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_PLANNED)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, shopping.id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = shopping.description.isEmpty() ? context.getString(R.string.reminder_title) : shopping.description;
        notificationManager.notify(shopping.id, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.md_primary))
                .setContentTitle(title)
                .setContentText(context.getString(R.string.reminder_text, shopping.time))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build());
    }
}
