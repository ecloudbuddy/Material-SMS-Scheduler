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
import android.os.CountDownTimer;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Kyle on 11/23/2015.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private static final String TAG = "RecyclerAdapter";
    private MessagesArrayList messages;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        private View mRemoveableView;

        // each data item is just a string in this case
        public TextView nameHeader;
        public TextView messageContentHeader;
        public TextView dateTimeHeader;
        public TextView timeHeader;
        public CircleImageView mBadge;

        public ViewHolder(View v) {
            super(v);
            mRemoveableView = itemView.findViewById(R.id.front);
            nameHeader = (TextView) v.findViewById(R.id.nameDisplay);
            messageContentHeader = (TextView) v.findViewById(R.id.messageContentDisplay);
            dateTimeHeader = (TextView) v.findViewById(R.id.dateTimeDisplay);
            mBadge = (CircleImageView) v.findViewById(R.id.circleImageView);
        }

        public View getSwipableView() {
            return mRemoveableView;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerAdapter(MessagesArrayList messages) {
        this.messages = messages;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        context = parent.getContext();
        v = LayoutInflater.from(context).inflate(R.layout.recycler_text_view, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.nameHeader.setText(messages.getNameDataset().get(position));
        holder.messageContentHeader.setText(messages.getContentDataset().get(position));

        ReadableDateCountdownTimer countdownTimer = new ReadableDateCountdownTimer(messages.getDateDataset().get(position), holder);
        countdownTimer.start();

        // Set image
        if (messages.getPhotoDataset().get(position) != null) {
            holder.mBadge.setImageBitmap(messages.getPhotoDataset().get(position));
        }

        holder.getSwipableView().bringToFront();
        holder.getSwipableView().setX(0);
        holder.getSwipableView().setY(0);

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return messages.size();
    }



    private class ReadableDateCountdownTimer extends CountDownTimer {
        private ViewHolder holder;
        private Calendar futureTime;
        GregorianCalendar dateToday, dateTomorrow, dateNextWeek;

        public ReadableDateCountdownTimer(Calendar futureTime, final ViewHolder holder) {
            super(futureTime.getTimeInMillis() - System.currentTimeMillis(), 1000);
            this.holder = holder;
            this.futureTime = futureTime;
            updateDates();
            updateHolderTextWithTimeUntilEvent();
        }

        /**Update all the date objects*/
        private void updateDates() {
            dateToday = getDateToday();
            dateTomorrow = getDateTomorrow();
            dateNextWeek = getDateNextWeek();
        }

        private GregorianCalendar getDateToday() {
            GregorianCalendar timeNow = new GregorianCalendar();
            GregorianCalendar dateToday = new  GregorianCalendar(timeNow.get(Calendar.YEAR), timeNow.get(Calendar.MONTH), timeNow.get(Calendar.DAY_OF_MONTH));
            dateToday.add(Calendar.DAY_OF_MONTH, 1);
            return dateToday;
        }

        private GregorianCalendar getDateTomorrow() {
            GregorianCalendar dateTomorrow = (GregorianCalendar) dateToday.clone();
            dateTomorrow.add(Calendar.DAY_OF_MONTH, 1);
            return dateTomorrow;
        }

        private GregorianCalendar getDateNextWeek() {
            GregorianCalendar dateNextWeek = (GregorianCalendar) dateToday.clone();
            dateNextWeek.add(Calendar.DAY_OF_MONTH, 7);
            return dateNextWeek;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateHolderTextWithTimeUntilEvent();
        }


        /**Get a readable string of the time remaining*/
        private void updateHolderTextWithTimeUntilEvent() {
            String textToDisplay;

            // If time equals midnight, then update all the dates
            if (isMidnight(futureTime)) {
                updateDates();
            }

            if (futureTime.before(dateToday) ) {
                textToDisplay = getReadableHoursSecondsUntilTime(futureTime);
            } else if(futureTime.before(dateTomorrow)) {
                textToDisplay = getTomorrowAndTime();
            } else if (futureTime.before(dateNextWeek)) {
                textToDisplay = getDayOfWeekAndTime();
            } else if (isOneWeekFromNow(futureTime)) {
                textToDisplay = context.getString(R.string.Next) + " " + getDayOfWeekAndTime();
            } else {
                textToDisplay = getDateAndTime();
            }
            holder.dateTimeHeader.setText(textToDisplay);
        }

        /**
         * Returns true if the time is midnight
         * @param time the time
         * @return true if the time is midnight
         */
        private boolean isMidnight(Calendar time) {
            return (time.get(Calendar.HOUR_OF_DAY) == 0) && (time.get(Calendar.MINUTE) == 0);
        }

        /**
         * Returns a readable string of hours/time until the future time
         * @param futureTime the future time.
         * @return a readable string of hours/time until the future time
         */
        private String getReadableHoursSecondsUntilTime(Calendar futureTime) {
            return DateUtils.getRelativeTimeSpanString(futureTime.getTimeInMillis(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS ).toString();
        }

        private String getTime() {
            int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME ;
            return DateUtils.formatDateTime(context, futureTime.getTimeInMillis(), flags);
        }

        /**
         * Gets a readable date/time string in the form "Tomorrow, 3pm"
         * @return a readable date/time string in the form "Tomorrow, 3pm"
         */
        private String getTomorrowAndTime() {
            return context.getString(com.simplicityapks.reminderdatepicker.lib.R.string.date_tomorrow) + ", " + getTime();
        }



        /**
         * Gets a readable date/time string in the form "Thursday, 3pm"
         * @return a readable date/time string in the form "Thursday, 3pm"
         */
        private String getDayOfWeekAndTime() {
            int flags = DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME;
            return DateUtils.formatDateTime(context, futureTime.getTimeInMillis(), flags);
        }

        /**
         * Gets a readable date/time string in the form "Oct 26, 3pm"
         * @return a readable date/time string in the form "Oct 26, 3pm"
         */
        private String getDateAndTime() {
            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME;
            return DateUtils.formatDateTime(context, futureTime.getTimeInMillis(), flags);
        }

        /**
         * Returns true if the time is one week from the current date.
         * @param futureTime The time to test
         * @return true if the time is one week from the current date.
         */
        private boolean isOneWeekFromNow(Calendar futureTime) {
            GregorianCalendar futureTimeIgnoringHours = (GregorianCalendar) futureTime.clone();
            futureTimeIgnoringHours.set(Calendar.HOUR_OF_DAY, 0);
            futureTimeIgnoringHours.set(Calendar.MINUTE, 0);
            return futureTimeIgnoringHours.equals(dateNextWeek);
        }

        @Override
        public void onFinish() {
            holder.dateTimeHeader.setText("Sending now ...");
            Log.i(TAG, "ReadableDateCountdownTimer finished. Should send now.");
        }
    }
}