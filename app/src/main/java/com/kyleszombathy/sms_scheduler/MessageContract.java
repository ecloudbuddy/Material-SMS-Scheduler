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
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PHONE = "phone";
        public static final String COLUMN_NAME_NAME_PHONE_FULL= "namePhoneFull";
        public static final String COLUMN_NAME_MESSAGE = "message";
        public static final String COLUMN_NAME_YEAR = "year";
        public static final String COLUMN_NAME_MONTH = "month";
        public static final String COLUMN_NAME_DAY = "day";
        public static final String COLUMN_NAME_HOUR = "hour";
        public static final String COLUMN_NAME_MINUTE = "minute";
        public static final String COLUMN_NAME_ALARM_NUMBER = "alarmNumber";
        public static final String COLUMN_NAME_SENT = "sent";
        public static final String COLUMN_NAME_NULLABLE = "nullable";
    }
}