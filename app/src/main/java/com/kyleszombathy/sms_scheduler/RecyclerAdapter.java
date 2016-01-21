package com.kyleszombathy.sms_scheduler;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Kyle on 11/23/2015.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private static final String TAG = "RecyclerAdapter";
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

        // each data item is just a string in this case
        public TextView nameHeader;
        public TextView messageContentHeader;
        public TextView dateHeader;
        public TextView timeHeader;
        public CircleImageView mBadge;

        public ViewHolder(View v) {
            super(v);
            mRemoveableView = itemView.findViewById(R.id.front);
            nameHeader = (TextView) v.findViewById(R.id.nameDisplay);
            messageContentHeader = (TextView) v.findViewById(R.id.messageContentDisplay);
            dateHeader = (TextView) v.findViewById(R.id.dateDisplay);
            timeHeader = (TextView) v.findViewById(R.id.timeDisplay);
            mBadge = (CircleImageView) v.findViewById(R.id.circleImageView);
        }



        public View getSwipableView() {
            return mRemoveableView;
        }
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
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_text_view, parent, false);
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
        return nameDataset.size();
    }
}