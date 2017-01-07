/*
 Copyright 2016 Kyle Szombathy

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.kyleszombathy.sms_scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 * This BroadcastReceiver automatically (re)starts the alarm when the device is
 * rebooted. This receiver is set to be disabled (android:enabled="false") in the
 * application's manifest file. When the user sets the alarm, the receiver is enabled.
 * When the user cancels the alarm, the receiver is disabled, so that rebooting the
 * device will not trigger this receiver.
 */
// BEGIN_INCLUDE(autostart)
public class MessageBootReceiver extends BroadcastReceiver {
    private static final String TAG = "MessageBootReceiver";
    private MessageAlarmReceiver alarm = new MessageAlarmReceiver();

    private ArrayList<String> nameDataset = new ArrayList<>();
    private ArrayList<String> phoneDataset = new ArrayList<>();
    private ArrayList<String> messageContentDataset = new ArrayList<>();
    private ArrayList<Calendar> calendarDataset = new ArrayList<>();
    private ArrayList<Integer> alarmNumberDateset = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.i(TAG, "Received Boot completed message from system");

            readFromSQLDatabase(context);
            for (int i = 0; i < phoneDataset.size(); i++) {
                ArrayList<String> phoneNumbers = parsePhoneNumbers(phoneDataset.get(i));
                ArrayList<String> names = parsePhoneNumbers(nameDataset.get(i));
                alarm.createAlarm(
                        context,
                        calendarDataset.get(i),
                        phoneNumbers,
                        messageContentDataset.get(i),
                        alarmNumberDateset.get(i),
                        names);
            }
        }
    }

    // Parses phone number string for phone numbers
    private ArrayList<String> parsePhoneNumbers(String phoneList) {
        ArrayList<String> phoneNumbers = new ArrayList<>();
        // Trims brackets off end
        phoneList = phoneList.replace("[", "");
        phoneList = phoneList.replace("]", "");
        Collections.addAll(phoneNumbers, phoneList.split(","));
        return phoneNumbers;
    }

    private void readFromSQLDatabase(Context context) {
        SQLDbHelper mDbHelper = new SQLDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                SQLContract.MessageEntry.NAME,
                SQLContract.MessageEntry.PHONE,
                SQLContract.MessageEntry.MESSAGE,
                SQLContract.MessageEntry.YEAR,
                SQLContract.MessageEntry.MONTH,
                SQLContract.MessageEntry.DAY,
                SQLContract.MessageEntry.HOUR,
                SQLContract.MessageEntry.MINUTE,
                SQLContract.MessageEntry.ALARM_NUMBER,
                SQLContract.MessageEntry.ARCHIVED
        };

        // How you want the results sorted in the resulting Cursor
        Cursor cursor = db.query(
                SQLContract.MessageEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Moves to first row
        cursor.moveToFirst();

        for (int i = 0; i < cursor.getCount(); i++) {
            int archived = cursor.getInt(cursor.getColumnIndexOrThrow
                    (SQLContract.MessageEntry.ARCHIVED));
            if (archived == 0) {
                // Pull data from sql
                int year = cursor.getInt(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.YEAR));
                int month = cursor.getInt(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.MONTH));
                int day = cursor.getInt(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.DAY));
                int hour = cursor.getInt(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.HOUR));
                int minute = cursor.getInt(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.MINUTE));
                nameDataset.add(cursor.getString(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.NAME)));
                messageContentDataset.add(cursor.getString(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.MESSAGE)));
                phoneDataset.add(cursor.getString(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.PHONE)));
                alarmNumberDateset.add(cursor.getInt(cursor.getColumnIndexOrThrow
                        (SQLContract.MessageEntry.ALARM_NUMBER)));

                // Set calendar database
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                calendarDataset.add(cal);
            }
            // Move to next row
            cursor.moveToNext();
        }
        // Close everything
        cursor.close();
        mDbHelper.close();
    }
}
//END_INCLUDE(autostart)