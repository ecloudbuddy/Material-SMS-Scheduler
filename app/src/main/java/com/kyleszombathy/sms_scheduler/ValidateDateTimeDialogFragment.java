package com.kyleszombathy.sms_scheduler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

/**
 * Created by Kyle on 10/16/2016.
 */

public class ValidateDateTimeDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.ValidateDateTimeDialogTitle2)
                .setMessage(R.string.ValidateDateTimeDialogMessage2)
                .setPositiveButton(R.string.YES,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((AddMessage)getActivity()).validateDateTimePositiveClick();
                            }
                        }
                )
                .setNegativeButton(R.string.picker_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing
                            }
                        }
                )
                .create();
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
    }


}
