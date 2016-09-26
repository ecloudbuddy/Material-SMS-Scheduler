package com.kyleszombathy.sms_scheduler;

import android.graphics.Bitmap;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Custom holder for Message type
 */
public class MessagesArrayList extends ArrayList<Message>{
    protected static MessagesArrayList instance;

    public MessagesArrayList() {
        // Do nothing
    }

    public static MessagesArrayList getInstance(){
        if (instance == null){
            instance = new MessagesArrayList();
        }
        return instance;
    }

    public ArrayList<String> getNameDataset() {
        ArrayList<String> nameList = new ArrayList<>();
        for (Message message : this) {
            nameList.add( extractName(message.getNameList()) );
        }
        return nameList;
    }

    public ArrayList<String> getContentDataset() {
        ArrayList<String> contentList = new ArrayList<>();
        for (Message message : this) {
            contentList.add( message.getContent() );
        }
        return contentList;
    }

    public ArrayList<String> getDateDataset() {
        ArrayList<String> dateList = new ArrayList<>();
        for (Message message : this) {
            dateList.add( extractReadableDate(message.getDateTime()) );
        }
        return dateList;
    }

    public ArrayList<Bitmap> getPhotoDataset() {
        ArrayList<Bitmap> bitmapList = new ArrayList<>();
        for (Message message : this) {
            bitmapList.add( message.getContactPhoto() );
        }
        return bitmapList;
    }

    /**Extracts name from given string */
    private String extractName(ArrayList<String> nameList) {
        //TODO: Write or find library that dynamically changes the message based on screen size
        StringBuilder nameCondensed = new StringBuilder(100);

        if(nameList.size() > 1) {
            nameCondensed.append(nameList.remove(0));
            nameCondensed.append(", ");
        }
        nameCondensed.append(nameList.remove(0));

        if (nameList.size() > 0) {
            nameCondensed.append(", +");
            nameCondensed.append(nameList.size());
        }
        return nameCondensed.toString();
    }

    /**Get full date string in a human readable format*/
    public static String extractReadableDate(Calendar inputDate){
        CharSequence readableSequence;
        GregorianCalendar dateNow = new GregorianCalendar();
        readableSequence = DateUtils.getRelativeTimeSpanString(
                inputDate.getTimeInMillis(),
                dateNow.getTimeInMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        return readableSequence.toString();
    }

    public int getAlarmIndex(int alarmNumber) {
        for (int index = 0; index < this.size(); index++) {
            Message message = this.get(index);
            if (message.getAlarmNumber() == alarmNumber) {
                return index;
            }
        }
        return -1;
    }
}
