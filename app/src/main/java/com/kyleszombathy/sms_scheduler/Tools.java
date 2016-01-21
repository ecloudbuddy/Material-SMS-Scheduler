package com.kyleszombathy.sms_scheduler;


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
}
