package com.kyleszombathy.sms_scheduler;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Kyle on 11/23/2015.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private ArrayList<String> nameDataset;
    private ArrayList<String> messageContentDataset;
    private ArrayList<String> dateDataset;
    private ArrayList<String> timeDataSet;
    private ArrayList<String> uriDataSet;
    private ArrayList<Bitmap> photoDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        private View mRemoveableView;
        private View mRevealRightView;
        private View mRevealLeftView;

        // each data item is just a string in this case
        public TextView nameHeader;
        public TextView messageContentHeader;
        public TextView dateHeader;
        public TextView timeHeader;
        public CircleImageView mBadge;

        public ViewHolder(View v) {
            super(v);
            mRemoveableView = itemView.findViewById(R.id.front);
            mRevealRightView = itemView.findViewById(R.id.revealRight);
            mRevealLeftView = itemView.findViewById(R.id.revealLeft);
            nameHeader = (TextView) v.findViewById(R.id.nameDisplay);
            messageContentHeader = (TextView) v.findViewById(R.id.messageContentDisplay);
            dateHeader = (TextView) v.findViewById(R.id.dateDisplay);
            timeHeader = (TextView) v.findViewById(R.id.timeDisplay);
            mBadge = (CircleImageView) v.findViewById(R.id.circleImageView);
        }
        public View getSwipableView() {
            return mRemoveableView;
        }
        public View getmRevealRightView() {
            return mRevealRightView;
        }
        public void setRevealRightViewEnabled(Boolean value) {
            if (!value) {
                mRevealRightView.setVisibility(View.GONE);
            } else {
                mRevealRightView.setVisibility(View.VISIBLE);
            }
        }

        public View getmRevealLeftView() {
            return mRevealLeftView;
        }
    }

    public void add(int position, String name,
                    String messageContent,
                    String date,
                    String time) {
        nameDataset.add(position, name);
        messageContentDataset.add(messageContent);
        dateDataset.add(date);
        timeDataSet.add(time);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        nameDataset.remove(position);
        messageContentDataset.remove(position);
        dateDataset.remove(position);
        timeDataSet.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerAdapter(ArrayList<String> nameDataset,
                           ArrayList<String> messageContentDataset,
                           ArrayList<String> dateDataset,
                           ArrayList<String> timeDataSet,
                           ArrayList<String> uriDataSet,
                           ArrayList<Bitmap> photoDataset) {
        this.nameDataset = nameDataset;
        this.messageContentDataset = messageContentDataset;
        this.dateDataset = dateDataset;
        this.timeDataSet = timeDataSet;
        this.uriDataSet = uriDataSet;
        this.photoDataset = photoDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_text_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.nameHeader.setText(nameDataset.get(position));
        holder.messageContentHeader.setText(messageContentDataset.get(position));
        holder.dateHeader.setText(dateDataset.get(position));
        holder.timeHeader.setText(timeDataSet.get(position));

        // Set image
        if (photoDataset.get(position) != null) {
            holder.mBadge.setImageBitmap(photoDataset.get(position));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return nameDataset.size();
    }

    public void clear() {
        nameDataset.clear();
        messageContentDataset.clear();
        dateDataset.clear();
        timeDataSet.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<String> name, List<String> message, List<String> date, List<String> time) {
        nameDataset.addAll(name);
        messageContentDataset.addAll(message);
        dateDataset.addAll(date);
        timeDataSet.addAll(time);
        notifyDataSetChanged();
    }
}