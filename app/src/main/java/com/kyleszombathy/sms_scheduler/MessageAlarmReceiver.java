package com.kyleszombathy.sms_scheduler;

import android.app.Activity;
import android.app.AlarmManager;
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


/**
 * When the alarm fires, this WakefulBroadcastReceiver receives the broadcast Intent
 * and then starts the IntentService {@code MessageSchedulingService} to do some work.
 */
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

        // Get wakelock
        Intent service = new Intent(context, MessageAlarmReceiver.class);

        // Start the service, keeping the device awake while it is launching.
        Log.i("SimpleWakefulReceiver", "Starting service @ " + SystemClock.elapsedRealtime());
        startWakefulService(context, service);

        ArrayList<String> phoneList = intent.getStringArrayListExtra("pNum");
        String messageContent = intent.getStringExtra("message");
        int alarmNumber = intent.getIntExtra("alarmNumber", -1);
        ArrayList<String> nameList = intent.getStringArrayListExtra("nameList");

        boolean sendSuccessFlag = true;

            // Split message
            ArrayList<String> messageArrayList;
            messageArrayList = smsManager.divideMessage(messageContent);

            // Sends to multiple recipients
            for (int i=0; i < phoneList.size(); i++) {
                boolean result = sendSMSMessage(phoneList.get(i), messageArrayList);

                if (!result) {
                    sendSuccessFlag = false;
                } else {
                    //markAsSent(context, alarmNumber);
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

        // Send notification if message send successfull
        if (sendSuccessFlag) {
            sendNotification(context, nameList);
        }
        markAsSent(context, alarmNumber);
        // Release wakelock
        completeWakefulIntent(service);
    }

    private void markAsSent(Context context, int alarmNumber) {
            Tools.setAsArchived(context, alarmNumber);
            // Sends broadcast to Home
            Intent intent = new Intent("custom-event-name");
            // You can also include some extra data.
            intent.putExtra("alarmNumber", alarmNumber);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /** Sends the actual messaage and handles errors*/
    private boolean sendSMSMessage(String phoneNumber,
                                   final ArrayList<String> messageArrayList) {
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
            ArrayList<PendingIntent> sentPIList = new ArrayList<>(Collections.nCopies(size, sentPI));
            ArrayList<PendingIntent> deliveryPIList = new ArrayList<>(Collections.nCopies(size, deliverPI));
            smsManager.sendMultipartTextMessage(phoneNumber, null, messageArrayList, sentPIList, deliveryPIList);
        }
        return true;
    }

    /** Posts a notification indicating recipients*/
    private void sendNotification(Context context, ArrayList<String> nameList) {
        String message = Tools.createSentString(context, nameList);

        // Construct notification
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, Home.class), 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(context.getString(R.string.message_success))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentText(message);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

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
