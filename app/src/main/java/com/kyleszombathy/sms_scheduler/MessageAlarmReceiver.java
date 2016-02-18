package com.kyleszombathy.sms_scheduler;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Objects;


public class MessageAlarmReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "MessageAlarmReceiver";
    private AlarmManager alarm;
    private PendingIntent pendingIntent;
    private SmsManager smsManager = SmsManager.getDefault();
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private Context context;
    private String SENT = "sent";
    private String DELIVERED = "delivered";


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        boolean sendSuccessFlag = true;

        // Get wakelock
        Intent service = new Intent(context, MessageAlarmReceiver.class);

        // Start the service, keeping the device awake while it is launching.
        Log.i("SimpleWakefulReceiver", "Starting service @ " + SystemClock.elapsedRealtime());
        startWakefulService(context, service);

        // Get data from intent
        ArrayList<String> phoneList = intent.getStringArrayListExtra("pNum");
        String messageContent = intent.getStringExtra("message");
        int alarmNumber = intent.getIntExtra("alarmNumber", -1);
        ArrayList<String> nameList = intent.getStringArrayListExtra("nameList");

            // Split message, regardless if needed - just in case I have the message length number wrong
            ArrayList<String> messageArrayList;
            messageArrayList = smsManager.divideMessage(messageContent);

            // Sends to multiple recipients
            for (int i=0; i < phoneList.size(); i++) {
                // Send message and retrieve
                boolean result = sendSMSMessage(phoneList.get(i), messageArrayList);
                if (!result) {
                    sendSuccessFlag = false;
                }
            }

        /* Register for SMS send action */
        context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = "";
                final String[] TRANSMISSION_TYPE = {
                        "Transmission successful",
                        "Transmission failed",
                        "Radio off",
                        "No PDU defined",
                        "No service"};

                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        result = TRANSMISSION_TYPE[0];
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        result = TRANSMISSION_TYPE[1];
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        result = TRANSMISSION_TYPE[2];
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        result = TRANSMISSION_TYPE[3];
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        result = TRANSMISSION_TYPE[4];
                        break;
                }
                Log.i(TAG, result);
                // Handle error
                if (!Objects.equals(result, TRANSMISSION_TYPE[0])) {
                    //messageSendSuccess[0] = false;
                }
            }
        }, new IntentFilter(SENT));

        // Create notification message
        String notificationMessage = Tools.createSentString(context, nameList, sendSuccessFlag);

        // Send notification if message send successfull
        if (sendSuccessFlag) {
            sendNotification(context, notificationMessage, messageContent, true);
        } else {
            sendNotification(context, notificationMessage, messageContent, false);
        }
        // Archive, regardless of send success or not
        markAsSent(context, notificationMessage, alarmNumber);
        // Release wakelock
        completeWakefulIntent(service);
    }

    /** Sends the actual messaage and handles errors*/
    private boolean sendSMSMessage(String phoneNumber, final ArrayList<String> messageArrayList) {
        int size = messageArrayList.size();

        // Result
        final Boolean[] messageSendSuccess = {true};

        // Sent and delivery intents
        Intent sentIntent = new Intent(SENT);
        Intent deliveryIntent = new Intent(DELIVERED);

        /*Create Pending Intents*/
        PendingIntent sentPI = PendingIntent.getBroadcast(
                context.getApplicationContext(), 0, sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent deliverPI = PendingIntent.getBroadcast(
                context.getApplicationContext(), 0, deliveryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Sends single or multiple messages based off message length
        if (size == 1) {
            smsManager.sendTextMessage(phoneNumber, null, messageArrayList.get(0), sentPI, deliverPI);
        } else {
            // Create sending/delivery lists for sending to multiple recipients
            ArrayList<PendingIntent> sentPIList = new ArrayList<>(Collections.nCopies(size, sentPI));
            ArrayList<PendingIntent> deliveryPIList = new ArrayList<>(Collections.nCopies(size, deliverPI));
            smsManager.sendMultipartTextMessage(phoneNumber, null, messageArrayList, sentPIList, deliveryPIList);
        }
        return true;
    }

    /** Posts a notification indicating recipients*/
    private void sendNotification(Context context, String notificationMessage, String messageContent, boolean sendSuccessful) {
        // Construct notification
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, Home.class), 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(notificationMessage)
                        .setContentText(messageContent)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(messageContent));
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /** Marks the specific alarm number as sent and sends a broadcast to home
     * @param context The application context
     * @param notificationMessage The notification message for notification on home screen
     * @param alarmNumber The specific alarm number*/
    private void markAsSent(Context context, String notificationMessage, int alarmNumber) {
        // Set as archived
        Tools.setAsArchived(context, alarmNumber);
        // Send broadcast to Home
        Intent intent = new Intent("custom-event-name");
        intent.putExtra("alarmNumber", alarmNumber);
        intent.putExtra("notificationMessage", notificationMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /** Method to set a new alarm
     * @param context The app context
     * @param timeToSend The Time to send the message (in Calendar format)
     * @param phoneNumberList String Arraylist of phone numbers
     * @param messageContent The content of the message you want to send
     * @param alarmNumber Provide an identifier for the alarm
     * @param nameList The list of names, corresponding with the phone numbers*/
    public void setAlarm(Context context,
                         Calendar timeToSend,
                         ArrayList<String> phoneNumberList,
                         String messageContent,
                         int alarmNumber,
                         ArrayList<String> nameList) {
        this.context = context;

        // Creates new alarm
        alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intentAlarm = new Intent(context, MessageAlarmReceiver.class);

        // Add extras
        Bundle extras = new Bundle();
        extras.putStringArrayList("pNum", phoneNumberList);
        extras.putString("message", messageContent);
        extras.putInt("alarmNumber", alarmNumber);
        extras.putStringArrayList("nameList", nameList);
        intentAlarm.putExtras(extras);

        // Set alarm
        pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmNumber,
                intentAlarm,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.set(AlarmManager.RTC_WAKEUP, timeToSend.getTimeInMillis(), pendingIntent);

        // Enable {@code MessageBootReceiver} to automatically restart the alarm when the
        // device is rebooted.
        ComponentName receiver = new ComponentName(context, MessageBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
    // END_INCLUDE(set_alarm)
}
