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


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Tools {
    private static final String TAG = "Tools";
    //==============Bitmaps======================//

    /**Converts a drawable to a bitmap
     * @param drawable The drawable to convert*/
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

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
    //================Time and Date==============//
    /** Returns a calendar instance given a set of time values*/
    public static Calendar getNewCalendarInstance(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return cal;
    }

    //================Strings and Arrays==============//
    /**Turns the output of an Arraylist.toString method back to an arraylist*/
    public static ArrayList<String> stringToArrayList(String s) {
        if(s == null) {
            return null;
        }
        s = s.replace("[", "");
        s = s.replace("]", "");
        String[] chars = s.split(",");

        ArrayList<String> returnStr= new ArrayList(Arrays.asList(chars));
        Log.d(TAG, "stringToArrayList: Input string is " + s + ", returning " + returnStr);
        return returnStr;
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

    /**Creates a String containing a list of recipients based on the length inputed
     * @param context Application context
     * @param nameList list of names to parse
     * @param maxNotificationSize max number of names to show
     * @param sendSuccess Returns a failed message string if false*/
    public static String createSentString(Context context, ArrayList<String> nameList, int maxNotificationSize, boolean sendSuccess) {
        String message;
        if (sendSuccess) {
            // Construct message
            message = context.getString(R.string.Notifications_Success)
                    + createNameCondesnedList(nameList, maxNotificationSize, true);
        } else {
            message = context.getString(R.string.Notifications_MessageFailure)
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

    /**Creates a full date string in a format for sorting*/
    public static String getFullDateStr(int year, int month, int day, int hour, int minute) {
        GregorianCalendar date = new GregorianCalendar(year, month, day, hour, minute);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return format.format(date.getTime());
    }
}