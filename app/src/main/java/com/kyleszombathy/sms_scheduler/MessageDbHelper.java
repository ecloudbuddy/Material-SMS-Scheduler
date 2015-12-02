package com.kyleszombathy.sms_scheduler;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Kyle on 11/30/2015.
 * Message database SQLiteOpenHelper
 */
public class MessageDbHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MessageContract.MessageEntry.TABLE_NAME + " (" +
                    MessageContract.MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    MessageContract.MessageEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_PHONE + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_NAME_PHONE_FULL + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_MESSAGE + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_YEAR + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_MONTH + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_DAY + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_HOUR + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_MINUTE + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_TIME_FULL + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_DATE_FULL + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_NULLABLE + TEXT_TYPE +
                    " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MessageContract.MessageEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";

    public MessageDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}