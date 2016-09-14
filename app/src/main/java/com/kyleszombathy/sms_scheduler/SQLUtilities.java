package com.kyleszombathy.sms_scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class for SQL DB add/retrieve
 */
public class SQLUtilities {
    private static final String TAG = "SQLUtilities";

    //=========================Setters======================//
    /** Adds data to sql db*/
    public static void addDataToSQL(Context context,
                                    ArrayList<String> name,
                                    ArrayList<String> phone,
                                    String messageContentString,
                                    int year, int month, int day, int hour, int minute,
                                    int alarmNumber,
                                    ArrayList<String> photoUri) {
        // SQLite database accessor
        SQLDbHelper mDbHelper = new SQLDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SQLContract.MessageEntry.NAME, name.toString());
        values.put(SQLContract.MessageEntry.PHONE, phone.toString());
        values.put(SQLContract.MessageEntry.MESSAGE, messageContentString);
        values.put(SQLContract.MessageEntry.YEAR, year);
        values.put(SQLContract.MessageEntry.MONTH, month);
        values.put(SQLContract.MessageEntry.DAY, day);
        values.put(SQLContract.MessageEntry.HOUR, hour);
        values.put(SQLContract.MessageEntry.MINUTE, minute);
        values.put(SQLContract.MessageEntry.ALARM_NUMBER, alarmNumber);
        values.put(SQLContract.MessageEntry.PHOTO_URI, photoUri.toString());
        values.put(SQLContract.MessageEntry.ARCHIVED, 0);
        values.put(SQLContract.MessageEntry.DATETIME, Tools.getFullDateStr(year, month, day, hour, minute));

        // Insert the new row, returning the primary key value of the new row
        long sqlRowId = db.insert(
                SQLContract.MessageEntry.TABLE_NAME,
                SQLContract.MessageEntry.NULLABLE,
                values);

        Log.i(TAG, "addDataToSQL: Added the following values to SQL: " + values.toString());
        mDbHelper.close();
    }

    //=========================Getters======================//
    public static InputStream getPhoto(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "getPhoto: photo is null for photoUri " + photoUri.toString());
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    Log.i(TAG, "getPhoto: photo retrieved successfully for photoUri " + photoUri.toString());
                    Log.d(TAG, "getPhoto: data is " + Arrays.toString(data));
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    //===================Delete/Archive==================//

    /**Sets given item as archived in database
     * @param context Application Context
     * @param alarmNumb Alarm number to archive*/
    public static void setAsArchived(Context context, int alarmNumb) {
        SQLDbHelper mDbHelper = new SQLDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(SQLContract.MessageEntry.
                ARCHIVED, 1);

        // Which row to update, based on the ID
        String selection = SQLContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumb) };

        int count = db.update(
                SQLContract.MessageEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        Log.i(TAG, "setAsArchived: "+ count + " rows deleted.");
        mDbHelper.close();
    }

    /**Removes Item given alarm Number from database
     * @param context Application context
     * @param alarmNumb The alarm number to delete*/
    public static void deleteFromDB(Context context, int alarmNumb) {
        try {
            SQLDbHelper mDbHelper = new SQLDbHelper(context);
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Which row to delete, based on the ID
            String selection = SQLContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
            String[] selectionArgs = { String.valueOf(alarmNumb) };

            db.delete(
                    SQLContract.MessageEntry.TABLE_NAME,
                    selection,
                    selectionArgs);
            mDbHelper.close();
            Log.i(TAG, "deleteFromDB: Alarm Deleted");
        } catch(Exception e) {
            Log.e(TAG, "deleteFromDB: Exception encountered", e);
        }
    }
}
