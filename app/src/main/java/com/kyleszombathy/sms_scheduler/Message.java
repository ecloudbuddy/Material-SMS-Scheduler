package com.kyleszombathy.sms_scheduler;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Kyle on 9/13/2016.
 */
public class Message {

    private ArrayList<String> nameList;
    private ArrayList<String> phoneList;
    private Calendar dateTime;
    private String content;
    private int alarmNumber;
    private ArrayList<Uri> uriList;
    private Bitmap contactPhoto;

    public Message(){
        nameList = new ArrayList<>();
        phoneList = new ArrayList<>();
        dateTime = new GregorianCalendar();
        content = "";
        alarmNumber = -1;
        uriList = new ArrayList<>();
        contactPhoto = null;
    }

    public Message(ArrayList<String> name, ArrayList<String> phone, int year, int month, int day, int hour, int minute, String messageContentString, int alarmNumber, ArrayList<Uri> uriList) {
        this.nameList = name;
        this.phoneList = phone;
        dateTime = new GregorianCalendar(year, month, day, hour, minute);
        this.content = messageContentString;
        this.alarmNumber = alarmNumber;
        this.uriList = uriList;
        contactPhoto = null;
    }

    public Message(ArrayList<String> name, ArrayList<String> phone, int year, int month, int day, int hour, int minute, String messageContentString, int alarmNumber, ArrayList<Uri> uriList, Bitmap contactPhoto) {
        this.nameList = name;
        this.phoneList = phone;
        dateTime = new GregorianCalendar(year, month, day, hour, minute);
        this.content = messageContentString;
        this.alarmNumber = alarmNumber;
        this.uriList = uriList;
        this.contactPhoto = contactPhoto;
    }

    public Message(ArrayList<String> name, ArrayList<String> phone, Calendar dateTime, String messageContentString, int alarmNumber, ArrayList<Uri> uriList) {
        this.nameList = name;
        this.phoneList = phone;
        this.dateTime = dateTime;
        this.content = messageContentString;
        this.alarmNumber = alarmNumber;
        this.uriList = uriList;
    }

    // Setters
    public void setNameList(ArrayList<String> nameList) {
        this.nameList = nameList;
    }

    public void setPhoneList(ArrayList<String> phoneList) {
        this.phoneList = phoneList;
    }

    public void setDateTime(Calendar dateTime) {
        this.dateTime = dateTime;
    }

    public void setYear(int year) {
        dateTime.set(Calendar.YEAR, year);
    }

    public void setMonth(int month) {
        dateTime.set(Calendar.MONTH, month);
    }

    public void setDay(int day) {
        dateTime.set(Calendar.DAY_OF_MONTH, day);
    }

    public void setHour(int hour) {
        dateTime.set(Calendar.HOUR_OF_DAY, hour);
    }

    public void setMinute(int minute) {
        dateTime.set(Calendar.MINUTE, minute);
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAlarmNumber(int alarmNumber) {
        this.alarmNumber = alarmNumber;
    }

    public void setUriList(ArrayList<Uri> uriList) {
        this.uriList = uriList;
    }

    public void setPhotoUriString(ArrayList<String> uriStrList) {
        uriList.clear();
        uriList = stringListToUriList(uriStrList);
    }

    public void setContactPhoto(Bitmap contactPhoto) {
        this.contactPhoto = contactPhoto;
    }

    // Getters
    public ArrayList<String> getNameList() {
        return nameList;
    }

    public ArrayList<String> getPhoneList() {
        return phoneList;
    }

    public Calendar getDateTime() {
        return dateTime;
    }

    public int getYear() {
        return dateTime.get(Calendar.YEAR);
    }

    public int getMonth() {
        return dateTime.get(Calendar.MONTH);
    }

    public int getDay() {
        return dateTime.get(Calendar.DAY_OF_MONTH);
    }

    public int getHour() {
        return dateTime.get(Calendar.HOUR_OF_DAY);
    }

    public int getMinute() {
        return dateTime.get(Calendar.MINUTE);
    }

    public String getContent() {
        return content;
    }

    public int getAlarmNumber() {
        return alarmNumber;
    }

    public ArrayList<Uri> getUriList() {
        return uriList;
    }

    public ArrayList<String> getPhotoUriStrList() {
        ArrayList<String> uriStrList = new ArrayList<>();
        for (Uri uri : uriList) {
            if (uri == null) {
                uriStrList.add(null);
            } else {
                uriStrList.add(uri.toString());
            }
        }
        return uriStrList;
    }

    public Bitmap getContactPhoto() {
        return contactPhoto;
    }

    // List utils
    public void addToNameList(String nameToAdd) {
        nameList.add(nameToAdd);
    }

    public void addToPhoneList(String phoneToAdd) {
        phoneList.add(phoneToAdd);
    }

    public void addToUriList(Uri uriToAdd) {
        uriList.add(uriToAdd);
    }

    public void addToUriStrList(String uriStrToAdd) {
        uriList.add(Uri.parse(uriStrToAdd));
    }

    public void clearLists(){
        nameList.clear();
        phoneList.clear();
        uriList.clear();
    }

    /** Use this method if you have an ArrayList of Uri in String form but need a Uri form
     * @param strList String ArrayList
     * @return Uri ArrayList
     */
    public static ArrayList<Uri> stringListToUriList (ArrayList<String> strList) {
        ArrayList<Uri> uriList = new ArrayList<>();
        for (String uriStr : strList) uriList.add(Uri.parse(uriStr.trim()));
        return uriList;
    }
}
