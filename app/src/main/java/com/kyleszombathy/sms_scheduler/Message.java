package com.kyleszombathy.sms_scheduler;

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

    public Message(){
        final String emptyStr = "";
        nameList = new ArrayList<>();
        phoneList = new ArrayList<>();
        dateTime = new GregorianCalendar();
        content = emptyStr;
        alarmNumber = -1;
        uriList = new ArrayList<>();
    }

    public Message(ArrayList<String> name, ArrayList<String> phone, String messageContentString, int alarmNumber) {
        this.nameList = name;
        this.phoneList = phone;
        dateTime = new GregorianCalendar();
        this.content = messageContentString;
        this.alarmNumber = alarmNumber;
        uriList = null;
    }

    public Message(ArrayList<String> name, ArrayList<String> phone, int year, int month, int day, int hour, int minute, String messageContentString, int alarmNumber, ArrayList<Uri> photoUri) {
        this.nameList = name;
        this.phoneList = phone;
        dateTime.set(Calendar.YEAR, year);
        dateTime.set(Calendar.MONTH, month);
        dateTime.set(Calendar.DAY_OF_MONTH, day);
        dateTime.set(Calendar.HOUR, hour);
        dateTime.set(Calendar.MINUTE, minute);
        this.content = messageContentString;
        this.alarmNumber = alarmNumber;
        this.uriList = photoUri;
    }

    public Message(ArrayList<String> name, ArrayList<String> phone, Calendar dateTime, String messageContentString, int alarmNumber, ArrayList<Uri> photoUri) {
        this.nameList = name;
        this.phoneList = phone;
        this.dateTime = dateTime;
        this.content = messageContentString;
        this.alarmNumber = alarmNumber;
        this.uriList = photoUri;
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
        dateTime.set(Calendar.HOUR, hour);
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
        for (String uriStr : uriStrList) {
            uriList.add(Uri.parse(uriStr.trim()));
        }
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
        return dateTime.get(Calendar.HOUR);
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
}
