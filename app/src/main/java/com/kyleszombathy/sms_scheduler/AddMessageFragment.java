package com.kyleszombathy.sms_scheduler;


import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A placeholder fragment containing a simple view.
 */
public class AddMessageFragment extends Fragment {
    private static final String TAG = "AddMessageFragment";

    public AddMessageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "Creating fragment view");
        return inflater.inflate(R.layout.fragment_add_message, container, false);
    }

}
