package com.kyleszombathy.sms_scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
<<<<<<< HEAD
import android.util.Log;

import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.util.ArrayList;
=======
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
>>>>>>> 657b36e3228dbd46f1e89f8c0f70643f7b6a6074

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
                                    ArrayList<String> fullChipString,
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
        values.put(SQLContract.MessageEntry.NAME_PHONE_FULL, fullChipString.toString());
        values.put(SQLContract.MessageEntry.MESSAGE, messageContentString);
        values.put(SQLContract.MessageEntry.YEAR, year);
        values.put(SQLContract.MessageEntry.MONTH, month);
        values.put(SQLContract.MessageEntry.DAY, day);
        values.put(SQLContract.MessageEntry.HOUR, hour);
        values.put(SQLContract.MessageEntry.MINUTE, minute);
        values.put(SQLContract.MessageEntry.ALARM_NUMBER, alarmNumber);
        values.put(SQLContract.MessageEntry.PHOTO_URI, photoUri.toString());
        values.put(SQLContract.MessageEntry.ARCHIVED, 0);
<<<<<<< HEAD
        values.put(SQLContract.MessageEntry.DATETIME, Tools.getFullDateString(year, month, day, hour, minute));
=======
        values.put(SQLContract.MessageEntry.DATETIME, Tools.getFullDateStr(year, month, day, hour, minute));
>>>>>>> 657b36e3228dbd46f1e89f8c0f70643f7b6a6074

        // Insert the new row, returning the primary key value of the new row
        long sqlRowId = db.insert(
                SQLContract.MessageEntry.TABLE_NAME,
                SQLContract.MessageEntry.NULLABLE,
                values);

        Log.i(TAG, "addDataToSQL: Added the following values to SQL: " + values.toString());
        mDbHelper.close();
    }

<<<<<<< HEAD
    public static void addPhotoDataToSQL(Context context,
                                   ArrayList<String> photoUri,
                                   DrawableRecipientChip[] chips) {
        SQLDbHelper mDbHelper = new SQLDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (int i = 0; i < chips.length; i++) {
            // Check if data already exists
            if (photoUri.get(i) != null) {
                if (SQLUtilities.getPhotoValuesFromSQL(context, photoUri.get(i)) == null) {
                    byte[] photoBytes = chips[i].getEntry().getPhotoBytes();

                    // Create a new map of values, where column names are the keys
                    ContentValues values = new ContentValues();
                    values.put(SQLContract.MessageEntry.
                            PHOTO_URI_1, photoUri.get(i));
                    values.put(SQLContract.MessageEntry.
                            PHOTO_BYTES, photoBytes);

                    // Insert the new row, returning the primary key value of the new row
                    long sqlRowId = db.insert(
                            SQLContract.MessageEntry.TABLE_PHOTO,
                            SQLContract.MessageEntry.NULLABLE,
                            values);
                    Log.i(TAG, "addPhotoDataToSQL: Contact photo added to DB. Row ID: " + String.valueOf(sqlRowId) + " URI: " + photoUri.get(i));
                } else {
                    Log.i(TAG, "addPhotoDataToSQL: Contact photo was not added to DB. Value already exists for " + chips[i].getEntry().toString());
                }
            } else {
                Log.e(TAG, "addPhotoDataToSQL: Contact photo for " + chips[i].getEntry().toString() + " was not found");
            }
        }
        // Close everything
        mDbHelper.close();
    }


    //=========================Getters======================//
    /**Given a uri, this method retrieves the photoBytes from the photoByte database
     * If the uri doesnt' exist in the database, it will return null
     * @param context Application context
     * @param uri A system uri to give*/
    public static byte[] getPhotoValuesFromSQL(Context context, String uri) {
        SQLDbHelper mDbHelper = new SQLDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                SQLContract.MessageEntry.PHOTO_URI_1,
                SQLContract.MessageEntry.PHOTO_BYTES
        };

        // Which row to update, based on the ID
        String selection = SQLContract.MessageEntry.PHOTO_URI_1 + " LIKE ?";
        String[] selectionArgs = { uri };

        byte[] photoBytes = null;

        try {
            Cursor cursor = db.query(
                    SQLContract.MessageEntry.TABLE_PHOTO,  // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                      // The sort order
            );
            if( cursor != null && cursor.moveToFirst() ) {
                cursor.moveToFirst();
                photoBytes = cursor.getBlob(cursor.getColumnIndex(SQLContract.MessageEntry.PHOTO_BYTES));
                cursor.close();
            } else {
                Log.e(TAG, "getPhotoValuesFromSQL: Could not retrieve contact photo");
            }
        } catch (Exception e) {
            Log.e(TAG, "getPhotoValuesFromSQL: Encountered exception", e);
        }

        mDbHelper.close();

        if (photoBytes == null) {
            return null;
        } else {
            return photoBytes;
        }
=======
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
>>>>>>> 657b36e3228dbd46f1e89f8c0f70643f7b6a6074
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
    public static void deleteAlarmNumberFromDatabase(Context context, int alarmNumb) {
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
            Log.i(TAG, "deleteAlarmNumberFromDatabase: Alarm Deleted");
        } catch(Exception e) {
            Log.e(TAG, "deleteAlarmNumberFromDatabase: Exception encountered", e);
        }
    }
}
