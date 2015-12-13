package com.kyleszombathy.sms_scheduler;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.Calendar;



/*
 * When the alarm fires, this WakefulBroadcastReceiver receives the broadcast Intent
 * and then starts the IntentService {@code MessageSchedulingService} to do some work.
 */
public class MessageAlarmReceiver extends WakefulBroadcastReceiver {
    private AlarmManager alarm;
    private PendingIntent pendingIntent;
    private SmsManager smsManager = SmsManager.getDefault();
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    @Override
    public void onReceive(Context context, Intent intent) {
        ArrayList<String> phone = intent.getStringArrayListExtra("pNum");
        String messageContent = intent.getStringExtra("message");
        int alarmNumber = intent.getIntExtra("alarmNumber", -1);

        System.out.println(phone.toString());
        System.out.println(messageContent);
        // Splits message
        ArrayList<String> messageArrayList;
        messageArrayList = smsManager.divideMessage(messageContent);

        // Sends to multiple recipients
        for (int i=0; i < phone.size(); i++) {
            sendSMSMessage(phone.get(i), messageArrayList);
            markAsSent(context, alarmNumber);
            sendNotification(context, "Message sent to ");
        }
    }

    private void markAsSent(Context context, int alarmNumber) {
        MessageDbHelper mDbHelper = new MessageDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(MessageContract.MessageEntry.COLUMN_NAME_SENT, 1);

        // Which row to update, based on the ID
        String selection = MessageContract.MessageEntry.COLUMN_NAME_ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumber) };

        int count = db.update(
                MessageContract.MessageEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    private void sendSMSMessage(String phoneNumber, ArrayList<String> messageArrayList) {
        // Sends single or multiple messages based off message length
        if (messageArrayList.size() == 1) {
            smsManager.sendTextMessage(phoneNumber, null, messageArrayList.get(0), null, null);
        } else {
            smsManager.sendMultipartTextMessage(phoneNumber, null, messageArrayList, null, null);
        }
    }

/*    private void extractName(String names) {
        String nameCondensedString = "";
        ArrayList<String> nameList = new ArrayList<>();
        names = names.replace("[", "");
        names = names.replace("]", "");

        if(names.contains(",")) {
            multipleRecipients = true;
            for (String name: names.split(",")) {
                nameList.add(name);
            }
            nameCondensedString = nameList.remove(0) + ", " + nameList.remove(0);
        } else {
            nameCondensedString = names;
        }

        int nameListSize = nameList.size();
        if (nameListSize > 0) {
            nameCondensedString += " +" + (nameListSize);
        }
        nameDataset.add(nameCondensedString);
    }*/

    // Post a notification indicating whether a doodle was found.
    private void sendNotification(Context context, String msg) {
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, Home.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(context.getString(R.string.message_success))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }


    // BEGIN_INCLUDE(set_alarm)
    /**
     * Sets a repeating alarm that runs once a day at approximately 8:30 a.m. When the
     * alarm fires, the app broadcasts an Intent to this WakefulBroadcastReceiver.
     * @param context
     */
    public void setAlarm(Context context, Calendar calendar,
                         ArrayList<String> phone, String messageContent,
                         int alarmNumber) {

        // Creates new alarm
        alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intentAlarm = new Intent(context, MessageAlarmReceiver.class);
        // Add extras
        Bundle extras = new Bundle();
        extras.putStringArrayList("pNum", phone);
        extras.putString("message", messageContent);
        extras.putInt("alarmNumber", alarmNumber);
        intentAlarm.putExtras(extras);

        pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmNumber,
                intentAlarm,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        // Enable {@code MessageBootReceiver} to automatically restart the alarm when the
        // device is rebooted.
        ComponentName receiver = new ComponentName(context, MessageBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
    // END_INCLUDE(set_alarm)

    /**
     * Cancels the alarm.
     * @param context
     */
    // BEGIN_INCLUDE(cancel_alarm)
    public void cancelAlarm(Context context) {
        // If the alarm has been set, cancel it.
        if (alarm != null) {
            alarm.cancel(pendingIntent);
        }

        // Disable {@code MessageBootReceiver} so that it doesn't automatically restart the
        // alarm when the device is rebooted.
        ComponentName receiver = new ComponentName(context, MessageBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
    // END_INCLUDE(cancel_alarm)
}
