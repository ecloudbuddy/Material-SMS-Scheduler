package com.kyleszombathy.sms_scheduler;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Kyle on 11/30/2015.
 * Message database SQLiteOpenHelper
 */
public class SQLDbHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BLOB_TYPE = " BLOB";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SQLContract.MessageEntry.TABLE_NAME + " (" +
                    SQLContract.MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    SQLContract.MessageEntry.NAME + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.PHONE + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.NAME_PHONE_FULL + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.MESSAGE + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.YEAR + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.MONTH + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.DAY + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.HOUR + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.MINUTE + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.ALARM_NUMBER + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.ARCHIVED + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.PHOTO_URI + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.NULLABLE + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.MMS + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.DATETIME + TEXT_TYPE +
                    " )";
    private static final String SQL_CREATE_ENTRIES_PHOTO =
            "CREATE TABLE " + SQLContract.MessageEntry.TABLE_PHOTO + " (" +
                    SQLContract.MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    SQLContract.MessageEntry.PHOTO_URI_1 + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.PHOTO_BYTES + BLOB_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.NULLABLE + TEXT_TYPE +
                    " )";
    private static final String SQL_CREATE_ENTRIES_NOTIFICATIONS =
            "CREATE TABLE " + SQLContract.MessageEntry.TABLE_NOTIFICATIONS + " (" +
                    SQLContract.MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    SQLContract.MessageEntry.NAME_LIST + TEXT_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.SEND_SUCCESS + INTEGER_TYPE + COMMA_SEP +
                    SQLContract.MessageEntry.NULLABLE + TEXT_TYPE +
                    " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SQLContract.MessageEntry.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_PHOTO =
            "DROP TABLE IF EXISTS " + SQLContract.MessageEntry.TABLE_PHOTO;
    private static final String SQL_DELETE_ENTRIES_NOTIFICATIONS =
            "DROP TABLE IF EXISTS " + SQLContract.MessageEntry.TABLE_NOTIFICATIONS;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "MessageDB.db";

    public SQLDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES_PHOTO);
        db.execSQL(SQL_CREATE_ENTRIES_NOTIFICATIONS);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_DELETE_ENTRIES_PHOTO);
        db.execSQL(SQL_DELETE_ENTRIES_NOTIFICATIONS);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}