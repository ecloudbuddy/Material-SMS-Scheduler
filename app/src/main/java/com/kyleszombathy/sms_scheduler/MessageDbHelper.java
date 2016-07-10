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
    private static final String BLOB_TYPE = " BLOB";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MessageContract.MessageEntry.TABLE_NAME + " (" +
                    MessageContract.MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    MessageContract.MessageEntry.NAME + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.PHONE + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.NAME_PHONE_FULL + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.MESSAGE + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.YEAR + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.MONTH + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.DAY + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.HOUR + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.MINUTE + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.ALARM_NUMBER + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.ARCHIVED + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.PHOTO_URI + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.NULLABLE + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.MMS + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.DATETIME + TEXT_TYPE +
                    " )";
    private static final String SQL_CREATE_ENTRIES_PHOTO =
            "CREATE TABLE " + MessageContract.MessageEntry.TABLE_PHOTO + " (" +
                    MessageContract.MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    MessageContract.MessageEntry.PHOTO_URI_1 + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.PHOTO_BYTES + BLOB_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.NULLABLE + TEXT_TYPE +
                    " )";
    private static final String SQL_CREATE_ENTRIES_NOTIFICATIONS =
            "CREATE TABLE " + MessageContract.MessageEntry.TABLE_NOTIFICATIONS + " (" +
                    MessageContract.MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    MessageContract.MessageEntry.NAME_LIST + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.SEND_SUCCESS + INTEGER_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.NULLABLE + TEXT_TYPE +
                    " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MessageContract.MessageEntry.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_PHOTO =
            "DROP TABLE IF EXISTS " + MessageContract.MessageEntry.TABLE_PHOTO;
    private static final String SQL_DELETE_ENTRIES_NOTIFICATIONS =
            "DROP TABLE IF EXISTS " + MessageContract.MessageEntry.TABLE_NOTIFICATIONS;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "MessageDB.db";

    public MessageDbHelper(Context context) {
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