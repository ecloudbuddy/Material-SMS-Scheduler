package com.kyleszombathy.sms_scheduler;

import android.provider.BaseColumns;

/**
 * Created by Kyle on 11/30/2015.
 * Creates SQLite table for storing contact information and time to send
 */
public final class MessageContract {



    public MessageContract() {}

    /* Inner class that defines the table contents */
    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "scheduledMessage";
        public static final String NAME = "name";
        public static final String PHONE = "phone";
        public static final String NAME_PHONE_FULL = "namePhoneFull";
        public static final String MESSAGE = "message";
        public static final String YEAR = "year";
        public static final String MONTH = "month";
        public static final String DAY = "day";
        public static final String HOUR = "hour";
        public static final String MINUTE = "minute";
        public static final String ALARM_NUMBER = "alarmNumber";
        public static final String ARCHIVED = "archived";
        public static final String PHOTO_URI = "photoUri";
        public static final String NULLABLE = "nullable";
        public static final String DATETIME = "dateTime";
        public static final String MMS = "MMS";
        // Photo DB table
        public static final String TABLE_PHOTO = "photoTable";
        public static final String PHOTO_URI_1 = "photoUri1";
        public static final String PHOTO_BYTES = "photoBytes";
        // Notifications Table
        public static final String TABLE_NOTIFICATIONS = "notificationsTable";
        public static final String NAME_LIST = "nameList";
        public static final String SEND_SUCCESS = "sendSuccess";
    }
}