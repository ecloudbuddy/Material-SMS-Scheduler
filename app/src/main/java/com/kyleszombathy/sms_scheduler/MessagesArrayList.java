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
    private ArrayList<String> nameDataset;
    private ArrayList<String> contentDataset;
    private ArrayList<String> dateDataset;
    private ArrayList<Bitmap> photoDataset;

    public MessagesArrayList() {
        super();
        nameDataset = new ArrayList<>();
        contentDataset = new ArrayList<>();
        dateDataset = new ArrayList<>();
        photoDataset = new ArrayList<>();
    }

    @Override
    public Message set(int index, Message message) {
        super.set(index, message);
        setDatasets(index);
        return message;
    }

    private void setDatasets(int index) {
        setContentDataset(index);
        setNameDataset(index);
        setDateDataset(index);
        setPhotoDataset(index);
    }

    @Override
    public Message remove(int index) {
        removeDatasetsIndex(index);
        return super.remove(index);
    }

    private void removeDatasetsIndex(int index) {
        nameDataset.remove(index);
        contentDataset.remove(index);
        dateDataset.remove(index);
        photoDataset.remove(index);
    }

    @Override
    public boolean add(Message message) {
        addToDatasets(message);
        return super.add(message);
    }

    private void addToDatasets(Message message) {
        nameDataset.add(extractReadableName(message.getNameList()));
        contentDataset.add(message.getContent());
        dateDataset.add(extractReadableDate(message.getDateTime()));
        photoDataset.add(message.getContactPhoto());
    }

    @Override
    public void add(int index, Message message) {
        addToDatasets(index, message);
        super.add(index, message);
    }

    private void addToDatasets(int index, Message message) {
        nameDataset.add(index, extractReadableName(message.getNameList()));
        contentDataset.add(index, message.getContent());
        dateDataset.add(index, extractReadableDate(message.getDateTime()));
        photoDataset.add(index, message.getContactPhoto());
    }

    @Override
    public void clear() {
        super.clear();
        clearDatasets();
    }

    private void clearDatasets() {
        nameDataset.clear();
        contentDataset.clear();
        dateDataset.clear();
        photoDataset.clear();
    }

    public void setNameDataset(int index) {
        nameDataset.set(index, extractReadableName(this.get(index).getNameList()));
    }

    public void setContentDataset(int index) {
        contentDataset.set(index, this.get(index).getContent());
    }

    public void setDateDataset(int index) {
        dateDataset.set(index, extractReadableDate(this.get(index).getDateTime()));
    }

    public void setPhotoDataset(int index) {
        photoDataset.set(index, this.get(index).getContactPhoto());
    }

    public void updateNameDataset() {
        nameDataset.clear();
        for (Message message : this) {
            nameDataset.add( extractReadableName(message.getNameList()) );
        }
    }

    public void updateContentDataset() {
        contentDataset.clear();
        for (Message message : this) {
            contentDataset.add( message.getContent() );
        }
    }

    public void updateDateDatset() {
        dateDataset.clear();
        for (Message message : this) {
            dateDataset.add( extractReadableDate(message.getDateTime()) );
        }
    }

    public void updatePhotoDataset() {
        photoDataset.clear();
        for (Message message: this) {
            photoDataset.add( message.getContactPhoto() );
        }
    }

    public ArrayList<String> getNameDataset() {
        return nameDataset;
    }

    public ArrayList<String> getContentDataset() {
        return contentDataset;
    }

    public ArrayList<String> getDateDataset() {
        return dateDataset;
    }

    public ArrayList<Bitmap> getPhotoDataset() {
        return photoDataset;
    }

    /**Extracts name from given string */
    private static String extractReadableName(ArrayList<String> nameList) {
        ArrayList nameListCopy = (ArrayList) nameList.clone(); // Must create new object or the old one will be referenced
        //TODO: Write or find library that dynamically changes the message based on screen size
        StringBuilder nameCondensed = new StringBuilder();

        if(nameListCopy.size() > 1) {
            nameCondensed.append(nameListCopy.remove(0));
            nameCondensed.append(", ");
        }
        nameCondensed.append(nameListCopy.remove(0));

        if (nameListCopy.size() > 0) {
            nameCondensed.append(", +");
            nameCondensed.append(nameListCopy.size());
        }
        return nameCondensed.toString();
    }

    /**Get full date string in a human readable format*/
    private static String extractReadableDate(Calendar inputDate){
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
