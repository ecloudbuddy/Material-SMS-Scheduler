package com.kyleszombathy.sms_scheduler;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class Tools {
    private static final String TAG = "Tools";

    public static ArrayList<String> parseString(String s) {
        if(s == null) {
            return null;
        }
        s = s.replace("[", "");
        s = s.replace("]", "");
        String[] chars = s.split(",");
        return new ArrayList(Arrays.asList(chars));
    }

    public static String parseArrayList(ArrayList<String> arrayList) {
        String returnString = "";
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            String str = arrayList.get(i).trim();
            if (i < size - 1) {
                returnString += str + " ";
            } else {
                returnString += str;
            }
        }
        return returnString;
    }

    /**Converts a drawable to a bitmap
     * @param drawable The drawable to convert*/
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**Given a uri, this method retrieves the photoBytes from the photoByte database
     * If the uri doesnt' exist in the database, it will return null
     * @param context Application context
     * @param uri A system uri to give*/
    public static byte[] getPhotoValuesFromSQL(Context context, String uri) {
        MessageDbHelper mDbHelper = new MessageDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                MessageContract.MessageEntry.PHOTO_URI_1,
                MessageContract.MessageEntry.PHOTO_BYTES
        };

        // Which row to update, based on the ID
        String selection = MessageContract.MessageEntry.PHOTO_URI_1 + " LIKE ?";
        String[] selectionArgs = { uri };

        byte[] photoBytes = null;

        try {
            Cursor cursor = db.query(
                    MessageContract.MessageEntry.TABLE_PHOTO,  // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                      // The sort order
            );
            if( cursor != null && cursor.moveToFirst() ) {
                cursor.moveToFirst();
                photoBytes = cursor.getBlob(cursor.getColumnIndex
                        (MessageContract.MessageEntry.PHOTO_BYTES));

                cursor.close();
            } else {
                Log.e(TAG, "Cursor Empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "SQLException", e);
        }

        db.close();
        mDbHelper.close();

        if (photoBytes == null) {
            return null;
        } else {
            return photoBytes;
        }
    }

    /**Creates a String containing a list of recipients based on the length inputed
     * @param context Application context
     * @param nameList list of names to parse
     * @param maxNotificationSize max number of names to show
     * @param sendSuccess Returns a failed message string if false*/
    public static String createSentString(Context context, ArrayList<String> nameList, int maxNotificationSize, boolean sendSuccess) {
        String message;
        if (sendSuccess) {
            // Construct message
            message = context.getString(R.string.tools_sentMessageSuccessString)
                    + createNameCondesnedList(nameList, maxNotificationSize, true);
        } else {
            message = context.getString(R.string.tools_sentMessageFailedString)
                    + createNameCondesnedList(nameList, maxNotificationSize, true);
        }
        return message;
    }

    /**Creates a condensed list of names. For example "Name1, Name2, +3"
     * @param nameList list of names to parse
     * @param maxNotificationSize max number of names to show
     * @param needsPeriod Should a period be shown if a "+" isn't needed. For example if there are 2 names only: "Name1, Name2."*/
    public static String createNameCondesnedList(ArrayList<String> nameList, int maxNotificationSize, boolean needsPeriod) {
        String message = "";
        maxNotificationSize -= 1;
        int size = nameList.size();

        if (size == 1) {
            message += nameList.get(0);
        } else if (size > 1) {
            for (int i = 0; i < size; i++) {
                message += nameList.get(i);
                if (i < size - 1) {
                    message += ", ";
                }
                // Display a max of 3 names with a +x message
                if (i == maxNotificationSize) {
                    int plusMore = size - maxNotificationSize - 1;
                    message += "+" + plusMore;
                    needsPeriod = false;// Display no period if +x is shown
                    break;
                }
            }
        }
        if (needsPeriod) {
            message += ".";
        }
        return message;
    }

    /**Sets given item as archived in database
     * @param context Application Context
     * @param alarmNumb Alarm number to archive*/
    public static void setAsArchived(Context context, int alarmNumb) {
        MessageDbHelper mDbHelper = new MessageDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(MessageContract.MessageEntry.
                ARCHIVED, 1);

        // Which row to update, based on the ID
        String selection = MessageContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumb) };

        int count = db.update(
                MessageContract.MessageEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        Log.i(TAG, count + " rows deleted.");
        db.close();
        mDbHelper.close();
    }

    /**Removes Item given alarm Number from database
     * @param context Application context
     * @param alarmNumb The alarm number to delete*/
    public static void deleteAlarmNumberFromDatabase(Context context, int alarmNumb) {
        MessageDbHelper mDbHelper = new MessageDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Which row to delete, based on the ID
        String selection = MessageContract.MessageEntry.ALARM_NUMBER + " LIKE ?";
        String[] selectionArgs = { String.valueOf(alarmNumb) };

        db.delete(
                MessageContract.MessageEntry.TABLE_NAME,
                selection,
                selectionArgs);
        db.close();
        mDbHelper.close();
    }


}
