package com.kyleszombathy.sms_scheduler;

import android.os.CountDownTimer;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Kyle on 11/23/2015.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private static final String TAG = "RecyclerAdapter";
    private MessagesArrayList messages;

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
        v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_text_view, parent, false);
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

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public ReadableDateCountdownTimer(long millisInFuture, long countDownInterval, final ViewHolder holder) {
            super(millisInFuture, countDownInterval);
            this.holder = holder;
        }

        public ReadableDateCountdownTimer(Calendar futureTime, final ViewHolder holder) {
            super(futureTime.getTimeInMillis() - System.currentTimeMillis(), 1000);
            this.holder = holder;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Long currentTimeInMillis = System.currentTimeMillis();
            holder.dateTimeHeader.setText(DateUtils.getRelativeTimeSpanString(
                    millisUntilFinished + currentTimeInMillis,
                    currentTimeInMillis,
                    DateUtils.SECOND_IN_MILLIS).toString());
        }

        @Override
        public void onFinish() {
            holder.dateTimeHeader.setText("Sending now ...");
        }
    }
}